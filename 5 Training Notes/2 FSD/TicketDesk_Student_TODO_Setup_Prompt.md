# Claude Prompt — Set Up the Student TODO Version of TicketDesk

Run this in Claude Code, inside the TicketDesk project folder, AFTER the
trainer answer-key repo is already built and verified working. This prompt
creates a second, student-facing version of the project with specific
pieces deliberately left incomplete — it does not touch or break the
existing trainer version, which stays fully working as a live reference
demo.

---

## ROLE

You have a complete, working reference implementation of TicketDesk (the
trainer answer key — treat the current state of the repo as ground truth).
Your job now is to produce a **student starter version**: the same project,
with exactly 30 specific pieces of logic removed and replaced with guided
TODOs, so students can complete them live during the workshop. Everything
NOT on the list below must remain fully working, exactly as it is now —
chat, uploads, notifications, logging, security middleware, Swagger, tests,
Docker, all of it stays intact and functional in the student version too.
Only the 30 listed items get stubbed.

## 1. VERSIONING STRATEGY

Use a git branch, not a separate folder copy — this mirrors real team
workflow and avoids two divergent codebases going stale against each other:
- Keep the current fully-working state committed on `main` (or a branch
  named `trainer-reference`) — this is the trainer's private answer key,
  never given to students
- Create a new branch `student-start` from that point
- Make all the stubbing changes described below ON `student-start` only
- `main`/`trainer-reference` remains runnable at all times — the trainer
  can `git checkout main` to demo the finished product, then `git checkout
  student-start` to show the class the version they'll actually receive

If running two instances side-by-side is useful for the trainer (finished
version on one port, student-start version on another, both running at
once during the demo), set `student-start`'s `.env.example` to use
different default ports than the trainer version, so both can run
simultaneously without conflict if the trainer wants to flip between them
live.

## 2. HOW TO STUB EACH ITEM — RULES

For every item in the list below:
- **Actually remove the working logic** — do not just comment out working
  code and call it a TODO. Replace it with either a minimal stub (e.g. a
  function that returns a placeholder value or throws
  `Error('Not implemented')`) or an empty scaffold, whichever keeps the
  rest of the app running without crashing on unrelated features
- **Leave the surrounding scaffold intact** — function signature, imports,
  the file structure — so students are extending a real file, not starting
  from a blank one
- **Point at an analogous already-built example in the same codebase**
  wherever one exists (e.g. the Admin branch of the permission matrix is
  built — Agent/Requester are TODO; the first aggregation stage is built —
  the second is TODO) — the TODO comment should explicitly say "follow the
  pattern in X"
- **The app must still start and run** with all 30 TODOs left untouched —
  routes that depend on a stubbed piece should degrade gracefully (return a
  clear "not implemented" response) rather than crashing the whole server
  or breaking unrelated features
- Use this exact comment format for every stub:
```js
// TODO(STUDENT): <what to build>
// Pattern to follow: <file path + what already-built example to mirror>
// Test it: <how to verify in Postman/browser once done>
```

## 3. STUDENT TODO SCOPE — BACKEND + DATABASE (16 items)

Database is intentionally folded into Backend here — at this project's
scale there's no separate DB layer; schema, indexing, and query decisions
are part of writing good backend code, and the stub list should reflect
that.

1. Agent branch of the permission matrix (`middlewares/permit.js`)
2. Requester branch of the permission matrix (`middlewares/permit.js`)
3. Remaining ticket workflow transitions + role guards
   (`utils/ticketStateMachine.js`)
4. Retrofit 2 endpoints to the shared response/error format (pick 2 that
   still use raw try-catch)
5. Input validation on the ticket-creation endpoint
   (express-validator/Joi — matching the pattern used elsewhere)
6. Rate limiting on a second endpoint beyond login (e.g. ticket creation)
7. Wire the chat → notification bridge for offline participants
8. Implement `GET /api/health` (basic health check, no auth)
9. One unit test for a workflow transition not already covered by an
   existing test
10. Required custom header check (`x-request-id`-style) on a second
    endpoint beyond the one already built
11. Add the correct `ref` relation + `.populate()` on a chosen endpoint
    that currently returns unpopulated IDs
12. Design and add a compound index for a given access pattern (state the
    access pattern in the TODO comment; students must justify which fields
    to index, not just copy an example)
13. Write the second dashboard aggregation stage (average resolution time
    or count-by-priority — your choice)
14. Implement the search-filter logic inside the shared query-builder
    helper
15. Implement the sort logic inside the shared query-builder helper
16. Implement the pagination (skip/limit) logic inside the shared
    query-builder helper

## 4. STUDENT TODO SCOPE — FRONTEND (14 items)

1. `TicketContext` provider + `useTicketContext()` hook
2. `FilterBar` component (`useState` for filter values)
3. `Pagination` component (`useState` for current page)
4. `useEffect` data fetching on the ticket-list page, refetching when
   filters/page change
5. Correct cleanup/`AbortController` usage in a `useEffect` (pick one
   fetch that currently lacks it)
6. `NotificationBell` dropdown open/close state + click-outside-to-close
   handling
7. `ChatBox` message input state + socket `emit` on send
8. `AttachmentUploader` file-select state + upload progress display
9. Wire `AppLayout` into two pages that don't currently use it
10. Loading/skeleton state on one data-fetching page
11. A custom hook (e.g. `useTickets` or `usePagination`) extracting logic
    currently duplicated across two components
12. Client-side validation feedback on the create/edit ticket `Modal` form
13. Socket.io connection lifecycle in React — connect on login, disconnect
    on logout (a common real-world bug source if done carelessly)
14. Empty-state UI ("No tickets found") on the ticket-list page, tied to
    the actual fetched data

## 5. COMPANION STUDENT WORKSHEET

Generate `TicketDesk_Student_TODO_Worksheet.md` at the repo root — a
checklist document students (and the trainer, for informal scoring) can
use during the workshop:
- Two sections: **Backend + Database (16)** and **Frontend (14)**
- Each item: a checkbox, the item number, a one-line description (not the
  full TODO comment text — a student-facing summary), the file path to
  open, and a "how to verify it's done" line (Postman request to run, or
  UI behavior to check)
- Keep this genuinely usable as a live-session checklist — short lines,
  skimmable, no long paragraphs

## 6. TRAINER DEMO CHECKLIST (BEFORE STUDENTS START)

Add a short section to the same worksheet file, above the checklist,
titled "Before You Start — Trainer Demo": a condensed version of the
existing Live Demo Runbook (from `TicketDesk_Trainer_Explainer.html`) —
the exact sequence to run the FINISHED (`trainer-reference` branch)
version live first, so students see the target behavior (login as each
role, walk a ticket through the full workflow, show chat, show a
notification arrive in real time, show the dashboard) before switching to
`student-start` and beginning the build. This is what lets students
picture the destination before they start writing code toward it.

## 7. SELF-CHECK BEFORE YOU FINISH

- [ ] `main`/`trainer-reference` branch still runs perfectly, completely
      unaffected by this work
- [ ] `student-start` branch runs without crashing with all 30 TODOs
      untouched — verify by actually starting the app in that state
- [ ] Every one of the 30 items has the exact TODO comment format from
      section 2, including a pattern-to-follow pointer
- [ ] No item's real implementation is still present anywhere in
      `student-start` (commented out or otherwise) — it must actually be
      gone, not hidden
- [ ] `TicketDesk_Student_TODO_Worksheet.md` exists, lists all 30 items
      accurately against the real code, and includes the trainer demo
      section
- [ ] Everything NOT on the 30-item list (chat infra, uploads infra,
      notifications infra, logging, Swagger, Docker, existing tests) is
      still fully functional on `student-start`

## OUTPUT

Perform the branch setup and all stubbing directly in the repository.
Create `TicketDesk_Student_TODO_Worksheet.md` at the repo root on the
`student-start` branch.
