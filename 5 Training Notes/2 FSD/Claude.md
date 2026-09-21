# Claude Prompt — Build the TicketDesk Trainer Answer Key (Full Build, No TODOs)

Paste everything below into Claude Code (or a fresh Claude chat with file-creation
enabled) to build the **complete, working, trainer-only reference implementation**
of TicketDesk. This is NOT the student-facing repo — there are no TODOs, no
stubs, nothing left unfinished. Build every feature fully so it can be reviewed,
run end-to-end, and used as the source of truth before a separate later prompt
carves a subset of it into a student version with TODOs.

This is a private repository. Do not add student-facing comments, TODO markers,
or simplified explanations anywhere — write it the way you'd write it for a
real engineering team.

---

## ROLE

You are building a full-featured, enterprise-quality MERN reference application:
an internal support ticket / issue-management system called **TicketDesk**. It
will be used by a trainer to (1) verify the architecture works end-to-end,
(2) demo it live to 119 students before they build their own version, and
(3) serve as the answer key when a later prompt strips selected pieces out
into student exercises.

Build for **enterprise standard**: proper layering (routes → controllers →
services → models), no business logic in route handlers, no Axios calls
inside React components, consistent naming, meaningful comments explaining
*why* design decisions were made, and defensive error handling throughout.

## PROJECT OVERVIEW

**TicketDesk** — an internal support ticket / issue management system with
three roles (Admin, Agent, Requester), an enforced ticket workflow (not free
CRUD), real-time chat per ticket, file attachments, in-app notifications,
and full observability (logging).

## TECH STACK

- Backend: Node.js, Express.js, MongoDB (Mongoose)
- Real-time: Socket.io
- File uploads: Multer (local disk by default, storage-adapter pattern so it
  can swap to Cloudinary/S3 without touching calling code)
- Logging: Morgan (HTTP request logs) + Winston (structured app/error logs to
  file, separate from the business-level `ActivityLog` collection)
- Validation: `express-validator` (or Joi — pick one and use it consistently)
- Security: `helmet`, `express-rate-limit` (especially on auth endpoints),
  centralized global error-handling middleware
- API docs: `swagger-jsdoc` + `swagger-ui-express`, served at `/api-docs`
- Testing: Jest + Supertest
- Containerization: Docker + docker-compose (mongo + backend + frontend)
- Frontend: React (functional components + hooks), Axios, Socket.io-client
- Auth: JWT (access token; no refresh-token flow needed for this scope)

## 1. FOLDER STRUCTURE

```
ticketdesk/
├── backend/
│   ├── src/
│   │   ├── routes/
│   │   ├── controllers/
│   │   ├── services/
│   │   ├── middlewares/
│   │   ├── models/
│   │   ├── sockets/
│   │   ├── utils/
│   │   ├── config/
│   │   ├── docs/              # swagger.config.js + any OpenAPI fragments
│   │   └── app.js
│   ├── uploads/                # local dev storage for attachments (gitignored)
│   ├── logs/                   # winston output (gitignored)
│   ├── tests/
│   │   ├── unit/
│   │   └── integration/
│   ├── server.js
│   ├── Dockerfile
│   ├── .env.example
│   └── package.json
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── layouts/
│   │   ├── context/
│   │   ├── hooks/
│   │   ├── services/
│   │   ├── pages/
│   │   ├── sockets/            # socket client setup
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── Dockerfile
│   ├── .env.example
│   └── package.json
├── postman/
│   └── TicketDesk.postman_collection.json
├── docker-compose.yml
└── README.md
```

## 2. DATA MODELS (7 collections)

```
User
  - name, email (unique), passwordHash, role: enum[Admin, Agent, Requester]

Category
  - name, defaultPriority: enum[Low, Medium, High]

Ticket
  - title, description
  - status: enum[Open, Assigned, InProgress, Resolved, Closed, Reopened]
  - priority: enum[Low, Medium, High]
  - category: ref Category
  - createdBy: ref User
  - assignedTo: ref User (nullable)
  - attachments: [{ filename, url, mimeType, size, uploadedBy: ref User, uploadedAt }]
  - createdAt, updatedAt
  - INDEX: compound (status, assignedTo)
  - INDEX: single on createdBy

Comment
  - ticket: ref Ticket
  - author: ref User
  - message
  - createdAt

ActivityLog
  - ticket: ref Ticket
  - action (e.g. "status_changed", "assigned", "commented", "attachment_added")
  - performedBy: ref User
  - timestamp

ChatMessage
  - ticket: ref Ticket
  - sender: ref User
  - message
  - createdAt
  - (persisted store behind the real-time layer — see section 6)

Notification
  - recipient: ref User
  - type: enum[ticket_assigned, status_changed, new_comment, new_chat_message]
  - ticket: ref Ticket
  - message
  - read: boolean (default false)
  - createdAt
```

## 3. PERMISSION MATRIX — build ALL roles fully

| Action | Requester | Agent | Admin |
|---|---|---|---|
| Create ticket | ✅ | ❌ | ✅ |
| View own tickets | ✅ | — | ✅ |
| View all tickets | ❌ | ✅ | ✅ |
| Assign ticket | ❌ | ✅ (self only) | ✅ (any) |
| Change status | ❌ | ✅ (if assigned to them) | ✅ |
| Delete ticket | ❌ | ❌ | ✅ |
| Comment | ✅ (own tickets) | ✅ | ✅ |
| Upload attachment | ✅ (own tickets) | ✅ (assigned) | ✅ |
| Chat on ticket | ✅ (own tickets) | ✅ (assigned) | ✅ |

Build `middlewares/permit.js` with every branch of this matrix fully
implemented and enforced server-side (never trust the frontend to hide a
button as the only protection).

## 4. TICKET WORKFLOW STATE MACHINE — build all transitions fully

```
Open → Assigned → InProgress → Resolved → Closed
                                Closed → Reopened → Assigned
```
- Only Agent/Admin can trigger `InProgress → Resolved`
- Only Admin can trigger `Resolved → Closed`
- Any role who can view the ticket can trigger `Closed → Reopened`
- Illegal transitions return 400 via the shared error format, never a raw 500

Implement fully in `utils/ticketStateMachine.js`, wired into
`PATCH /api/tickets/:id/status`. Every transition should also write an
`ActivityLog` entry and trigger a `Notification` to the relevant user(s).

## 5. REUSABLE RESPONSE / ERROR FORMAT — apply everywhere

`utils/response.js`:
```js
success(res, data, message = 'OK', statusCode = 200)
fail(res, statusCode, message, details = null)
```
`middlewares/asyncHandler.js` wraps every controller — no repeated try-catch
blocks anywhere in the codebase. Apply this format consistently across every
single endpoint, including chat, attachments, and notifications.

## 6. LOGGING MIDDLEWARE

- **Morgan**: HTTP request logging (method, path, status, response time),
  written to console in dev and to `logs/access.log` in production
- **Winston**: structured application/error logging — separate log levels
  (info, warn, error), errors written to `logs/error.log`, all logs also to
  console in dev. Wire Winston into the centralized error-handling middleware
  (section 10) so every unhandled error is logged with context (route, user
  ID if authenticated, stack trace)
- Keep this explicitly distinct from `ActivityLog` in the comments: Winston/
  Morgan are technical/operational logs; `ActivityLog` is a business-level
  audit trail exposed to users in the product itself

## 7. MULTIMEDIA UPLOAD

- `Multer` middleware on `POST /api/tickets/:id/attachments`, accepting
  images and PDFs, with file-size limit (e.g. 5MB) and MIME-type validation
- Build a storage-adapter pattern: `services/storage/localStorage.js`
  implementing a common interface (`upload(file)`, `getUrl(filename)`,
  `delete(filename)`), used by default. Structure it so swapping in a
  `cloudStorage.js` (Cloudinary/S3) later requires no changes to calling code
- Attachment metadata stored on the `Ticket.attachments` array; the file
  itself lives in `backend/uploads/` (local) behind a static route or
  controller-served download endpoint — never expose the raw filesystem path
  to the client
- Reject invalid file types/oversized files via the shared error format

## 8. LIVE CHAT (Socket.io)

- One Socket.io room per ticket (`ticket:{ticketId}`), joined on the
  ticket-detail page mount and left on unmount
- Room membership/visibility enforced server-side using the same permission
  matrix as section 3 — a Requester should not be able to join a room for a
  ticket they don't own; verify the JWT during the socket handshake, not just
  on the initial page load
- Messages persisted to `ChatMessage` on send (REST-backed persistence, with
  Socket.io used purely for the real-time push) — the chat history should
  survive a page refresh via `GET /api/tickets/:id/chat`
- Sending a message via socket should also trigger a `Notification` for the
  other participant(s) if they're not currently connected to that room
- Document clearly in code comments why persistence + real-time coexist here
  (a common interview question)

## 9. IN-APP NOTIFICATIONS

- `Notification` collection (section 2), created automatically on: ticket
  assigned, status changed, new comment, new chat message (per section 8)
- `GET /api/notifications` (current user's notifications, paginated,
  unread-first) and `PATCH /api/notifications/:id/read`
- Push new notifications over the same Socket.io connection in real time
  (separate event/channel from chat messages) so the frontend bell updates
  live without polling
- Frontend: a notification bell in `AppLayout` topbar, unread count badge,
  dropdown list, click-to-mark-read

## 10. SECURITY & RELIABILITY

- `helmet` applied globally for security headers
- `express-rate-limit` on `POST /api/auth/login` specifically (e.g. 5
  attempts per 15 minutes per IP) — brute-force protection, returns the
  shared error format with a 429
- Centralized global error-handling middleware (last in the middleware
  chain) — catches anything `asyncHandler` passes to `next(err)`, logs it via
  Winston, and returns a consistent error response shape, never leaking stack
  traces to the client in production mode
- Input validation via `express-validator` (or Joi) on all write endpoints
  (auth, ticket create/update, comment, chat message, attachment) — reject
  with 400 + shared error format on invalid input, before it reaches the
  controller
- CORS configured from `FRONTEND_ORIGIN` env variable, not hardcoded

## 11. API DOCUMENTATION (Swagger/OpenAPI)

- `swagger-jsdoc` + `swagger-ui-express`, served at `/api-docs`
- JSDoc-style OpenAPI annotations above every route definition (method,
  params, request body schema, response schema, auth requirement)
- This is the *documentation* layer; the Postman collection (section 14) is
  the *testing* layer — keep both in sync

## 12. AUTOMATED TESTING (Jest + Supertest)

Build real, passing tests — not placeholders:
- **Unit tests** (`tests/unit/`): the ticket state-machine logic — every
  legal transition succeeds, every illegal transition throws/returns the
  expected error, for each role
- **Unit tests**: the permission-matrix middleware — table-driven tests
  covering every cell of the matrix in section 3
- **Integration tests** (`tests/integration/`, using Supertest against an
  in-memory or test MongoDB instance): login flow, create-ticket flow,
  full status-transition flow via the actual HTTP endpoints
- Add an `npm test` script; tests should be runnable with zero manual setup
  beyond `npm install`

## 13. DOCKERIZATION

- `backend/Dockerfile` — multi-stage if reasonable, production-lean image
- `frontend/Dockerfile` — build the React app, serve via a lightweight static
  server (e.g. `serve` or nginx)
- Root `docker-compose.yml` wiring: `mongo` (with a named volume),
  `backend` (depends_on mongo, env from `.env`), `frontend` (depends_on
  backend). Should come up with a single `docker-compose up` and be fully
  functional, including file uploads (mount `uploads/` as a volume) and
  Socket.io (expose the right port)

## 14. FRONTEND — build fully, no stubs

- `layouts/AppLayout.jsx` — Sidebar + Topbar (with notification bell) +
  `<Outlet />`
- `context/AuthContext.jsx` — full auth state + `useAuth()`
- `context/TicketContext.jsx` — full ticket list/filter state +
  `useTicketContext()`, avoiding prop-drilling into FilterBar/Pagination/list
- `components/`: `TicketCard`, `Modal` (generic, reused for create/edit
  ticket and confirm-delete), `FilterBar`, `Pagination`, `ChatBox` (per-ticket
  chat UI wired to Socket.io), `NotificationBell`, `AttachmentUploader`
- `pages/TicketList.jsx` — full `useEffect` data fetching, wired to
  `TicketContext` filters/pagination, refetches on filter/page change
- `pages/TicketDetail.jsx` — full `useEffect` fetching ticket + comments +
  activity log in parallel via `Promise.allSettled`, plus the live `ChatBox`
  and attachment list/upload
- `services/`: `ticketService.js`, `authService.js`, `chatService.js`,
  `notificationService.js` — every API call lives here, nowhere else
- `sockets/socketClient.js` — single Socket.io client instance, connected
  once auth succeeds, exposing subscribe/unsubscribe helpers for chat and
  notification events

## 15. API ENDPOINTS (full surface)

```
POST   /api/auth/login
GET    /api/tickets                       query: status, page, limit, sort, search
GET    /api/tickets/:id
POST   /api/tickets                       header: x-request-id required
PATCH  /api/tickets/:id/status
POST   /api/tickets/:id/assign
DELETE /api/tickets/:id
POST   /api/tickets/:id/comments
GET    /api/tickets/:id/comments
POST   /api/tickets/:id/attachments       multipart/form-data
GET    /api/tickets/:id/attachments/:fileId
GET    /api/tickets/:id/chat
GET    /api/notifications
PATCH  /api/notifications/:id/read
GET    /api/dashboard/summary             aggregation
GET    /api-docs                          Swagger UI
GET    /api/health                        basic health check, no auth required
```

## 16. POSTMAN COLLECTION

Generate `postman/TicketDesk.postman_collection.json` covering every endpoint
above, plus:
- Environment with `baseUrl` and `token` variables
- Login request with a test script that auto-saves the token
- Requests grouped by folder: Auth, Tickets, Attachments, Chat, Comments,
  Notifications, Dashboard, Health

## 17. DEPLOYMENT READINESS

- `.env.example` for backend and frontend, every variable documented
- Environment-based config split (`config/dev.js` / `config/prod.js` or
  equivalent) — not just one flat `.env`
- `README.md`: setup (local and Docker), how to run tests, how to import the
  Postman collection, where Swagger docs are served, architecture overview
  diagram (ASCII is fine)

## 18. SELF-CHECK BEFORE YOU FINISH

- [ ] Every file/folder in section 1 exists
- [ ] Zero TODOs, stubs, or placeholder logic anywhere in the codebase
- [ ] Full app works end-to-end for all three roles (Admin, Agent, Requester)
      — auth, ticket CRUD, every workflow transition, chat, attachments,
      notifications
- [ ] `npm test` passes with real, meaningful tests (not trivial assertions)
- [ ] `docker-compose up` brings up a fully working stack from a clean clone
- [ ] Swagger UI at `/api-docs` accurately reflects every endpoint
- [ ] Postman collection runs end-to-end via Collection Runner with zero
      manual token handling
- [ ] No Axios calls exist outside `frontend/src/services/`
- [ ] No business logic exists inside any Express route file
- [ ] Rate limiting, helmet, and centralized error handling are verifiably
      active (test the rate limit by hitting login 6+ times)

## OUTPUT

Build the complete repository as specified. This is a reference implementation
— write real, complete, working code for every section, not scaffolding.
