# Claude Prompt — Automated Grading for All 30 Student TODO Items

This build touches TWO existing projects — read both carefully before
starting:
1. The TicketDesk repo, on the `student-start` branch — you're adding a
   grading script here
2. The standalone Progress Tracker app (separate folder) — you're changing
   how it receives data: from manual checkboxes to automated verification

Run this in Claude Code with both project folders accessible. If they're
not both open in the same workspace, do the TicketDesk-side work first,
then the Progress-Tracker-side work, keeping the contract between them
(described in section 3) consistent across both.

---

## HONESTY NOTE FOR THE TRAINER (include this reasoning in your summary,
## don't silently skip it)

This system runs entirely on each student's own laptop, then reports
results to your machine. Like any self-hosted grading in a classroom
(not a locked-down exam environment), a technically determined student
could theoretically fake a passing result. The shared secret in section 4
is a reasonable deterrent for a training workshop, not a tamper-proof
guarantee. The goal here is to replace slow manual review at 300-student
scale with trustworthy-by-default automated checks — not to build exam
security. State this plainly in your build summary so expectations are set
correctly.

## 1. WHERE THE GRADING SCRIPT LIVES

Add a new folder to the TicketDesk repo, on `student-start`:
```
grading/
  self-check.js          # main entry point
  checks/
    backendChecks.js      # items 1-16 (Backend + DB)
    frontendChecks.js      # items 1-14 (Frontend, separate numbering from backend)
  config.js               # reads registration number + secret key
```
Add to the **repo root** `package.json` (not `backend/package.json` or
`frontend/package.json`): `"self-check": "node grading/self-check.js"`,
so students run `npm run self-check` from the repo root. This matters
because the script needs to orchestrate both backend and frontend
together — if it's added to either subfolder's `package.json` instead, it
won't be runnable the way students expect, and `npm run self-check` will
fail with "Missing script" when run from the wrong directory. Verify this
explicitly: confirm a root-level `package.json` exists (create a minimal
one if this repo doesn't already have one), and that the very first line
of the grading section in `README.md` states clearly: **"Run all grading
commands from the project root — not from inside `backend/` or
`frontend/`."**

On first run, if no registration number is saved locally (e.g. in a
gitignored `grading/.student.json`), prompt the student to enter it once
via CLI input, then save it so they don't retype it every run.

## 2. WHAT THE SCRIPT DOES, END TO END

**Students will typically already have their own `npm run dev` servers
running in another terminal while they work — the script must not assume
a clean slate, and must not force students to stop their dev servers
before checking progress.**

1. **Detect before starting anything**: check whether something is already
   listening on the expected backend port (and frontend port) — e.g. a
   quick connection attempt or hitting `/api/health`. If something's
   already running there, use it directly for the checks below and skip
   starting a new instance. Only start the backend/frontend as child
   processes if nothing is already running on those ports.
2. If the script did start its own instance(s), wait for them to be ready
   before proceeding
3. Runs all 16 backend checks (section 5)
4. Runs all 14 frontend checks (section 6)
5. **Only shuts down servers the script itself started** — never kill a
   server the student was already running, since that would interrupt
   their work
6. Prints a clear pass/fail summary table to the terminal, item by item,
   so the student gets immediate feedback without waiting for the
   dashboard
7. POSTs the full result set to the Progress Tracker app (section 4)
8. If the Progress Tracker isn't reachable (e.g. trainer's app isn't
   running yet), still print local results and retry the POST, or clearly
   tell the student to re-run `npm run self-check` once it's up — never
   silently fail to report

This means a student can run `npm run self-check` in a second terminal at
any point — mid-build, without stopping anything — and it should just work
against whatever's currently running, or spin up its own temporary
instance if nothing is.

**Backend and frontend checks must run fully independently of each
other's success or failure.** If the frontend fails to start (e.g. a
student hasn't built anything there yet, or it errors out), that must NOT
prevent the 16 backend checks from running and reporting normally — mark
only the frontend items as "not tested," report the backend results in
full. The same applies in reverse. A student who has finished backend but
not started frontend must get an accurate, complete backend report, not a
blank or failed one caused by the unrelated frontend being unready.

If ANY item can't be tested because the server itself failed to start,
report that clearly and mark all dependent items as "not tested" (not
"failed" and not "passed") — never guess.

## 3. RESULT CONTRACT (must match exactly on both sides)

```json
{
  "registrationNumber": "string",
  "results": [
    { "category": "backend", "itemNumber": 1, "passed": true },
    { "category": "backend", "itemNumber": 2, "passed": false },
    ...
    { "category": "frontend", "itemNumber": 14, "passed": true }
  ],
  "timestamp": "ISO string"
}
```
`itemNumber` for backend runs 1-16 matching the existing catalog order
already used in the Progress Tracker; frontend runs 1-14 the same way.
Reuse the exact item text already hardcoded in the Progress Tracker app —
do not renumber or reword anything.

## 4. PROGRESS TRACKER CHANGES

**New endpoint**: `POST /api/verify`
- Requires a shared secret in a header (e.g. `x-grading-key`), checked
  against a value in the Progress Tracker's own `.env` — generate a
  reasonably long random default and document it clearly in both READMEs,
  since the trainer needs to give students the same key (e.g. write it on
  the board, or bake it into the `student-start` branch's
  `grading/config.js` default before distributing the repo)
- On a valid request, upserts into `item_completions` for that
  registration number, setting `completed = passed` and a new `verified =
  true` column, plus `completed_at` on pass
- Reject silently-wrong payloads with a clear 400, don't crash

**Remove manual checkboxes from the student-facing page.** This is now a
status/report page, not an input form:
- Student still enters their registration number to view their own status
- The 30 items are displayed with their current pass/fail state, but as
  read-only indicators (e.g. green check / red cross / grey "not tested
  yet"), not clickable checkboxes
- Add a clear instruction on this page: *"Run `npm run self-check` in your
  project to update this."*
- If a registration number has never had a self-check run, show all 30
  items as "not tested yet," not as failed

**Dashboard, PDF export, CSV export**: keep the same structure as before
(section 4-5 of the original build), just sourced from `verified` data
now instead of self-reported checkboxes. Add a `Verified` column/indicator
so it's visually obvious this data came from automated checks, not manual
entry — useful if you ever need to explain the report's credibility to the
college.

## 5. BACKEND + DB CHECKS (items 1-16) — technique per item

Use Supertest/Axios against the running local server for all API-based
checks. Use a direct Mongoose/MongoDB connection for the index checks. Use
child-process test execution for the unit-test item. Read the actual repo
first to get correct route paths and payload shapes — do not guess field
names.

1. **Agent permission branch** — call an Agent-restricted action (e.g.
   assign) with an Agent token → expect 200; same call with a Requester
   token → expect 403
2. **Requester permission branch** — call a Requester-allowed action (e.g.
   create ticket, comment on own ticket) with a Requester token → expect
   200/201; a Requester-forbidden action → expect 403
3. **Remaining workflow transitions + role guards** — attempt each
   remaining transition (InProgress→Resolved, Resolved→Closed,
   Closed→Reopened, Reopened→Assigned) with correctly and incorrectly
   authorized roles, check status codes match the matrix
4. **Response/error format retrofit** — call the 2 endpoints the student
   was told to retrofit, check the response body matches the shared
   envelope shape (`success`/`data`/`message` or `error` fields)
5. **Input validation on ticket creation** — POST with a missing required
   field → expect 400 with the shared error format, not a 500
6. **Rate limiting on a second endpoint** — fire requests past the
   configured limit in quick succession → expect a 429 at some point
7. **Chat → notification bridge** — simulate an offline recipient (no
   active socket for that user), send a chat message, check a
   `Notification` record was created for them
8. **`GET /api/health`** — call it, expect 200 with no auth required
9. **New unit test exists** — run `npm test`, compare the test count/names
   against the trainer-reference baseline (bundled in the repo) to confirm
   a genuinely new test exists for an uncovered transition, not zero
   change
10. **Custom header check on a second endpoint** — call it without the
    required header → expect 400; with it → expect success
11. **`.populate()` on a chosen endpoint** — call it, check the response
    contains full nested objects (e.g. `createdBy.name`) rather than bare
    ObjectId strings
12. **Compound index exists** — connect directly to MongoDB, call
    `getIndexes()` on the `Ticket` collection, confirm a compound index on
    the expected fields is present
13. **Second aggregation stage** — call the dashboard summary endpoint,
    confirm the expected new field (e.g. average resolution time or
    count-by-priority) is present and numerically sane
14. **Search-filter query-builder logic** — call the ticket-list endpoint
    with a search/filter query param, confirm the returned results are
    actually filtered, not the full unfiltered list
15. **Sort logic** — call with a sort param, confirm the returned order is
    actually sorted correctly
16. **Pagination logic** — call with page/limit params, confirm the
    correct slice and count metadata are returned

## 6. FRONTEND CHECKS (items 1-14) — technique per item

Use Playwright (headless) against the running local frontend for
behavioral checks. For structural items, scan actual source files for the
expected pattern instead of driving the browser.

1. **`TicketContext`** — scan source for a context provider file and
   confirm it's imported/used by at least the ticket-list page (structural
   check)
2. **`FilterBar`** — in Playwright, change a filter control, confirm a
   network request fires with the updated query parameter
3. **`Pagination`** — in Playwright, click "next page," confirm a network
   request fires with the updated page parameter
4. **`useEffect` fetch on ticket-list page** — in Playwright, load the
   page, confirm a fetch request to the tickets endpoint actually occurs
5. **Cleanup/AbortController** — harder to test behaviorally; scan source
   for `AbortController`/cleanup-function usage in the relevant
   `useEffect`, treat as a structural check
6. **`NotificationBell` dropdown** — in Playwright, click the bell, confirm
   the dropdown becomes visible; click elsewhere, confirm it closes
7. **`ChatBox` send** — in Playwright, type a message and send, confirm a
   socket emit occurs (or the message appears in the DOM if that's
   easier to detect reliably) and the message field clears after sending
8. **`AttachmentUploader`** — in Playwright, select a file, confirm an
   upload request fires and a progress indicator appears
9. **`AppLayout` on two more pages** — scan source for the layout
   component being imported/used on the two specified pages
10. **Loading/skeleton state** — in Playwright, throttle or delay the
    fetch response, confirm a loading indicator renders before data
    appears
11. **Custom hook extracted** — scan source for a new hook file, confirm
    it's imported in 2+ places where the duplicated logic used to live
12. **Client-side validation on the Modal** — in Playwright, submit the
    form with a required field empty, confirm a validation message appears
    and the request is NOT sent
13. **Socket lifecycle** — in Playwright, log in, confirm a socket
    connection opens (check for the connection event or an open
    WebSocket); log out, confirm it closes
14. **Empty-state UI** — in Playwright, load the ticket list in a state
    with no matching tickets (e.g. via an obscure filter), confirm the
    empty-state message renders

## 7. SELF-CHECK BEFORE FINISHING

- [ ] A root-level `package.json` exists with the `self-check` script, and
      `npm run self-check` works when run from the repo root — the exact
      failure mode to check for is "Missing script: self-check," which
      means it landed in the wrong `package.json`
- [ ] `npm run self-check` runs start to finish with zero manual steps
      beyond entering a registration number once
- [ ] Running `npm run self-check` WHILE `npm run dev` is already active
      in another terminal works correctly — detects the running server,
      uses it, and does not crash or try to bind an already-used port
- [ ] Running `npm run self-check` with NOTHING already running also works
      — it starts its own temporary instance and cleanly shuts it down
      afterward
- [ ] Running `npm run self-check` with a working backend but a
      completely broken/unstarted frontend still reports full, accurate
      backend results — frontend failure does not blank out or block
      backend results, and vice versa
- [ ] Running it against the fully-completed trainer-reference branch
      (temporarily, for testing) reports all 30 as passed — verifies the
      checks aren't false-failing on correct code
- [ ] Running it against a completely untouched `student-start` (all 30
      TODOs still stubbed) reports all 30 as failed/not-tested — verifies
      the checks aren't false-passing on incomplete code
- [ ] Results actually appear on the Progress Tracker dashboard after a
      run, correctly attributed to the right registration number
- [ ] The student-facing Progress Tracker page shows read-only status, no
      clickable checkboxes remain anywhere
- [ ] The shared secret is required and enforced on `/api/verify` —
      requests without it are rejected
- [ ] PDF and CSV exports still work and now show verified data

## OUTPUT

Implement the grading script on the TicketDesk `student-start` branch and
the corresponding changes to the Progress Tracker app. Document the shared
secret key clearly in both READMEs.
