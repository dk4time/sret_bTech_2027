# Claude Prompt — Add 4 Data-Flow Walkthrough Chapters to the Trainer Explainer

Run this in Claude Code, inside the TicketDesk project folder.
`TicketDesk_Trainer_Explainer.html` already exists and works — **update it
in place**, adding new content. Do not regenerate the whole file and do not
alter existing sections except where explicitly noted below.

Everything in this prompt must be grounded in the **real code in this
repository** — read the actual files before writing any explanation or code
snippet. Do not invent function names, variable names, or logic that isn't
actually in the codebase. If a described step doesn't match what you find
in the real files, follow the real implementation, not this prompt's
assumption of it.

---

## 0. PLACEMENT

Add one new top-level sidebar entry: **"Data Flow Walkthroughs."** Under it,
add four chapters/tabs, one per flow below. Position this new top-level
entry directly after the existing "Request Trace" section, since it's the
same kind of content (an animated, step-by-step journey through real code)
applied to four different user journeys instead of one.

**Avoid duplicating content that already exists elsewhere in the doc.**
Specifically:
- The existing "Request Trace" section already covers the ticket
  status-change request in detail (auth middleware, permission middleware,
  controller, service, model, response) — the Login → Logout chapter below
  should NOT re-explain that same middleware chain in full; instead, cover
  login/logout specifically and link to or briefly reference the existing
  Request Trace section for the shared middleware pattern rather than
  repeating it
- The existing Socket Handshake section (added in a previous update) and
  the `io.js`/`chatSocket.js` explanation in Key Pattern Explanations
  already cover connection setup and room mechanics — the Live Chat chapter
  below should build on top of that, not re-explain the handshake or the
  singleton `io` pattern again; link back to those sections for that part
- If you find other genuine overlap while writing this, resolve it the
  same way: cover new material fully, reference existing material rather
  than duplicating it

## 1. INTERACTION MODEL — MATCH THE EXISTING REQUEST TRACE STYLE

Each of the four chapters must use the same animated step-reveal treatment
already built for the "Request Trace" section: a visual sequence of stages
(numbered, connected by arrows) where clicking or hovering a stage
highlights it (and dims the others) and reveals the real code for that
specific step. Reuse that existing component/pattern — do not build a
different interaction style for these four chapters.

At the end of each chapter, add one styled callout box — visually
consistent with the "one-line spoken explanation" callout already used in
the "Why Not the Obvious Way" section — containing a single memorable
sentence that ties the whole flow together for students. Use the callout
text given in each chapter outline below as a starting point, but adjust
it if the real code reveals a more accurate framing.

## 2. CHAPTER 1 — Login → Logout (Full Data Flow)

Read the actual auth-related files (routes, controller, service, User
model, `AuthContext`, the Axios service setup, `socketClient.js`) and build
the animated sequence around what's really there. Cover, as sequential
stages:
1. Login form submit → frontend service call → which endpoint, what body
2. Backend: route → controller → service → password check → JWT signed →
   response format used
3. Frontend: token received → where it's stored (`AuthContext`) → how the
   Axios instance attaches it to future requests automatically
4. Socket connects here, using the same token — link to the existing
   Socket Handshake section for what happens under the hood
5. Every subsequent protected request's shape (briefly — link to Request
   Trace for the full middleware breakdown rather than repeating it)
6. Logout: what actually gets cleared (context state, token, socket
   disconnect), in what order

Closing callout (adapt to match real code): *"Nothing after login works
without the token — it's the one thing every request from here to logout
depends on."*

## 3. CHAPTER 2 — Live Chat (Socket.io)

Read `chatSocket.js`, `ChatBox` (frontend), and the `ChatMessage` model.
Build the animated sequence:
1. Ticket-detail page mounts → `join-room` emitted with the real event/data
   shape used in this codebase
2. Backend: room join, permission re-check (reference, don't repeat, the
   handshake/permission content already covered elsewhere)
3. User sends a message → local state → `send-message` emitted
4. Backend: message persisted to `ChatMessage` AND broadcast to the room —
   show both actually happening, in the real code, side by side if the
   visual format supports it
5. Notification side-effect for offline participants — brief, link to
   Chapter 4 for the full notification explanation rather than repeating it
6. Cleanup: `leave-room` on unmount, full disconnect on logout

Closing callout: *"Sending a message does two things in one action —
writes to the database so it survives a refresh, and pushes to everyone in
the room so it feels instant. Neither replaces the other."*

## 4. CHAPTER 3 — Comments (Plain REST — the Deliberate Contrast)

Read the actual comment routes/controller/service/model and the frontend
comment component. Build the animated sequence:
1. Comment form submit → local state → service call → endpoint
2. Backend: auth → permission check → validation → controller → service →
   `Comment` saved → `ActivityLog` entry → `Notification` created
3. Frontend: response handling — re-fetch or optimistic update, and
   explicitly note there is NO live push here

Add a visually distinct comparison callout (styled differently from the
single-sentence callouts elsewhere — this one should feel like a labeled
side-by-side, e.g. two short columns or a small table) contrasting this
chapter with Chapter 2: **Comments = REST, ask-and-get-an-answer, refresh
to see others' comments** vs. **Chat = Socket, stay-connected,
instant-by-design**. Make clear this is a deliberate design choice in the
project, not an inconsistency.

## 5. CHAPTER 4 — Notifications (The Bridge Between the Other Three)

Read the actual notification model, service, socket handling, and
`NotificationBell` component. Build the animated sequence:
1. Trigger points in the real code — list the actual places notifications
   get created (ticket assigned, status changed, comment added, offline
   chat message) by finding where `notificationService`-equivalent is
   actually called
2. Backend: how the notification is saved, and how it's pushed in
   real time — cover the personal-room-per-user pattern if that's what the
   real code does (a room like `user:{userId}`, separate from ticket rooms)
   — if the actual implementation differs, describe what's really there
3. Frontend: `NotificationBell` listener → unread count → dropdown list
4. The "cold start" gap: what `GET /api/notifications` does on login to
   catch up on anything missed while offline, since the socket only
   delivers what happens while connected
5. Mark-as-read flow

Closing callout: *"The socket handles 'while you're here, right now.' The
REST fetch on login handles 'catch me up on what I missed.' You need both,
or notifications either don't survive a logout or don't feel real-time."*

## 6. SELF-CHECK BEFORE FINISHING

- [ ] All four chapters use the exact same animated step-reveal interaction
      as the existing Request Trace section — verify by clicking through
      them, not just visually comparing
- [ ] Every code snippet shown is copied from the real repository files,
      not invented or approximated
- [ ] No chapter fully re-explains content that already exists elsewhere in
      the document — cross-references/links are used instead
- [ ] Each chapter ends with its closing callout, styled consistently with
      the rest of the document
- [ ] The new "Data Flow Walkthroughs" entry appears correctly in the
      sidebar navigation, positioned after "Request Trace"
- [ ] Chapter 3's comparison callout is visually distinct from the standard
      single-line callouts, as specified

## OUTPUT

Update `TicketDesk_Trainer_Explainer.html` in place. Do not create a new
file.
