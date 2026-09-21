# Claude Prompt — Build the TicketDesk Progress Tracker (Local Classroom App)

Run this in Claude Code, in a NEW folder separate from the TicketDesk
project (this is a standalone trainer tool, not part of TicketDesk itself).
It has no dependency on TicketDesk's own database or backend — it's a
completely independent, small app.

---

## ROLE

Build a lightweight local web app that lets 119 students self-report their
progress on the 30-item TicketDesk Student TODO Worksheet, by entering
their registration number, and gives the trainer a live dashboard plus a
final exportable PDF report suitable for submission to the college as
evidence of workshop completion.

This must be simple and bulletproof — it runs on the trainer's laptop over
classroom WiFi for two full days with no internet dependency and no
external services. Prioritize reliability over features.

## TECH STACK (deliberately minimal — do not add more than this)

- Backend: Node.js + Express
- Database: SQLite via `better-sqlite3` — a single local file, no separate
  DB server to install or manage
- Frontend: plain HTML/CSS/JS, no framework, no build step — served
  directly as static files by Express
- PDF export: `pdfkit` (or similar, no external API/service)
- Everything must work fully offline after the initial `npm install` —
  classroom WiFi will not have real internet access during the workshop

## 1. ITEM CATALOG — hardcode exactly this list, do not alter or invent

**Backend + Database (16 items):**
1. Agent branch of the permission matrix
2. Requester branch of the permission matrix
3. Remaining ticket workflow transitions + role guards
4. Retrofit 2 endpoints to the shared response/error format
5. Input validation on the ticket-creation endpoint
6. Rate limiting on a second endpoint beyond login
7. Wire the chat → notification bridge for offline participants
8. Implement `GET /api/health`
9. One unit test for a workflow transition not already covered
10. Required custom header check on a second endpoint
11. Add the correct `ref` relation + `.populate()` on a chosen endpoint
12. Design and add a compound index for a given access pattern
13. Write the second dashboard aggregation stage
14. Implement search-filter logic in the query-builder helper
15. Implement sort logic in the query-builder helper
16. Implement pagination (skip/limit) logic in the query-builder helper

**Frontend (14 items):**
1. `TicketContext` provider + `useTicketContext()` hook
2. `FilterBar` component (`useState`)
3. `Pagination` component (`useState`)
4. `useEffect` data fetching on the ticket-list page
5. Correct cleanup/`AbortController` in a `useEffect`
6. `NotificationBell` dropdown state + click-outside handling
7. `ChatBox` message input state + socket emit on send
8. `AttachmentUploader` file-select state + upload progress
9. Wire `AppLayout` into two pages that don't use it yet
10. Loading/skeleton state on one data-fetching page
11. A custom hook extracting logic duplicated across two components
12. Client-side validation feedback on the create/edit ticket Modal
13. Socket.io connection lifecycle — connect on login, disconnect on logout
14. Empty-state UI on the ticket-list page

## 2. DATABASE SCHEMA

```
students
  - registration_number (TEXT, PRIMARY KEY)
  - created_at
  - updated_at

item_completions
  - registration_number (TEXT, FK -> students)
  - category (TEXT: 'backend' | 'frontend')
  - item_number (INTEGER)
  - completed (BOOLEAN)
  - completed_at (nullable timestamp)
  - PRIMARY KEY (registration_number, category, item_number)
```

## 3. STUDENT-FACING FLOW

- **Landing page**: a single input for registration number + a "Start /
  Resume" button. On submit:
  - If the registration number doesn't exist yet, create it
  - If it exists, load their previously saved progress — this matters
    because students return across Day 1 and Day 2 and must not lose
    prior progress
- **Checklist page**: the 30 items grouped into two clearly labeled
  sections (Backend + Database — 16, Frontend — 14), each with a checkbox,
  numbered and worded exactly as the catalog above
- **Auto-save**: checking/unchecking a box saves immediately via a small
  API call — no submit button anywhere on this page. Show a brief, subtle
  "Saved" confirmation on each check so students trust it's actually
  persisting
- **Running total** displayed at the top of the checklist at all times:
  "Backend+DB: x/16 · Frontend: x/14 · Total: x/30"
- Keep this page usable on a phone browser — students may be on laptops or
  phones on the classroom WiFi, layout should not break on a small screen

## 4. TRAINER DASHBOARD (`/admin` route, separate from the student flow)

- A table: Registration Number | Backend+DB (x/16) | Frontend (x/14) |
  Total (x/30) | Last Updated
- Sortable by any column (click header to sort)
- A search box to filter by registration number
- A manual refresh button (polling every few seconds is fine too, but keep
  it lightweight — this is running on a laptop, not a server)
- A **Delete** action per row, for cleaning up a duplicate/mistyped
  registration number entry without needing direct database access
- An optional small summary at the top: how many students have started,
  how many have completed all 30, class-wide average completion

## 5. PDF EXPORT

A button on the dashboard: **"Export Report (PDF)."** Generates a PDF
containing:
- A header with the workshop name and dates (make these easily editable —
  a config value at the top of the code, not hardcoded deep in the PDF
  generation logic)
- A table of every registration number with their Backend+DB, Frontend,
  and Total counts, sorted by registration number
- A generation timestamp
- Keep the layout clean and simple — this needs to look presentable
  handed directly to a college administrator, not like a debug dump

## 5b. CSV EXPORT (for opening directly in Excel)

A second button on the dashboard, next to the PDF export: **"Export Report
(CSV)."** Generates and downloads a `.csv` file with one row per student:
`Registration Number, Backend+DB Completed, Backend+DB Total, Frontend
Completed, Frontend Total, Total Completed, Total Items, Last Updated`.
This should open correctly in Excel/Google Sheets with no extra software
needed — use plain comma-separated values with a header row, no special
libraries required beyond basic string formatting or a minimal CSV helper.
Keep the column values identical to what the dashboard and PDF report show
— all three (dashboard, PDF, CSV) must always agree with each other.

## 6. LOCAL NETWORK HOSTING

- The Express server must bind to `0.0.0.0`, not just `localhost`, so
  other devices on the same WiFi network can reach it
- On startup, clearly log the local network URL to the console in a
  large, unmissable way — e.g.:
```
=================================================
  Students connect at: http://<local-ip>:3000
=================================================
```
  Detect and print the actual local network IP automatically (don't make
  the trainer look it up manually) — use Node's `os.networkInterfaces()`
  or equivalent
- Single command to start the whole thing: `npm start`, with clear
  `README.md` setup instructions (install once, then start each day)
- The SQLite file should persist across restarts (don't recreate the DB
  on every launch) so stopping/restarting the app between Day 1 and Day 2
  doesn't lose any data

## 7. SELF-CHECK BEFORE FINISHING

- [ ] `npm install` then `npm start` brings the whole app up with no
      additional steps
- [ ] The console clearly prints a working local-network URL on startup
- [ ] A student can enter a registration number, check items, refresh the
      page, and see their progress still there
- [ ] Two different registration numbers don't interfere with each other's
      data
- [ ] The trainer dashboard updates to reflect new checkbox activity
      without needing a server restart
- [ ] The PDF export produces accurate counts matching what's shown on the
      dashboard at the time of export
- [ ] The CSV export opens correctly in Excel/Google Sheets and its counts
      match the dashboard and PDF exactly
- [ ] The whole app works with the laptop's WiFi disconnected from the
      internet — only the local network matters
- [ ] The student-facing page is usable on a small/phone-sized screen

## OUTPUT

Build this as a complete, runnable standalone project in its own folder,
with a `README.md` covering setup and how to find the local network URL
each day.
