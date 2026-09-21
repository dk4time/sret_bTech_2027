# Claude Prompt — Generate an Interactive Trainer Explainer for the Built TicketDesk Repo

Run this in Claude Code, inside the TicketDesk project folder, AFTER the repo
has already been built. Point it at the actual codebase — every explanation
must reference real file paths and real code from this repo, not generic
MERN theory.

This document serves two purposes at once: the trainer's own self-study
before class, and a live, click-through teaching aid projected in front of
119 students. Build it as a single interactive HTML page, not a flat
document — see OUTPUT FORMAT below for the exact requirements.

---

## ROLE

You just built (or are examining) this TicketDesk repository. The person
running this prompt is the trainer who will explain this codebase, live, to
119 students over a 2-day workshop — but did not write it line-by-line
themselves and needs to fully understand it before teaching it. Your job is
to produce a document that lets them explain every part of this project
confidently, as if they wrote it.

Do not describe MERN stack concepts in the abstract. Every explanation must
point at actual files, actual function names, and actual code in this
specific repository.

## OUTPUT FORMAT — INTERACTIVE HTML, NOT MARKDOWN

This has two audiences at once: (1) the trainer, reading it alone beforehand
to actually understand the codebase, and (2) 119 students, watching it
projected live in class. Build a single self-contained interactive HTML
page — not a markdown/PDF document — that serves both.

**Structure:**
- A persistent left sidebar with a clickable table of contents (the 10
  sections below), so the trainer can jump straight to "Hour 3" mid-class
  without scrolling
- Each section is its own panel/view — clean, one topic on screen at a time,
  not a long scroll of everything at once (this is what makes it usable on
  a projector, unlike a wall of markdown text)
- Generous font sizes and high contrast by default — this will be read from
  the back of a room, not a laptop screen up close

**Interactivity (this is the core requirement, not decoration):**
- In the Folder Walkthrough and Cheat Sheet sections: hovering over a
  file/folder name reveals its purpose in a tooltip or side panel, without
  navigating away — lets the trainer explore live in response to a
  student's question instead of pre-scripting every click
- In the "Why Not the Obvious Way" section: each function/pattern entry
  should be collapsed by default (just the function name + a one-line
  teaser), expanding on click to reveal the full breakdown (obvious way /
  actual way / why / spoken line). This lets the trainer control pacing
  live — click to reveal, not everything dumped on screen at once
- In the Request Trace section (end-to-end walkthrough of the status-change
  request): render it as a visual step sequence (numbered stages, e.g.
  Route → Middleware → Controller → Service → Model → Response) where
  clicking or hovering each stage highlights/reveals the actual code for
  that step, so it reads like an animated flow rather than a static list
- Code snippets throughout: syntax-highlighted (use highlight.js or
  Prism from cdnjs), in collapsible `<details>`-style blocks so the page
  isn't overwhelming, expandable on demand
- A simple "Presentation Mode" toggle that enlarges text and simplifies
  spacing further, for when it's actually being projected vs. read
  up close
- Subtle animation on section transitions/reveals (fade or slide) — tasteful
  and fast, not distracting. This is a teaching tool, not a marketing site;
  motion should aid focus, not compete for attention

**Technical constraints:**
- Single HTML file, inline CSS and JS (or clearly separated `<style>`/
  `<script>` blocks within the same file) — must work by opening the file
  directly in a browser, no build step, no server required, since it needs
  to run reliably in a classroom with 119 students' machines and whatever
  the trainer's laptop has installed
- No external dependencies beyond CDN-hosted libraries (e.g. highlight.js/
  Prism via cdnjs) — nothing that requires npm install to view it
- Keep it performant — this is one long document's worth of content; don't
  render everything expanded in the DOM by default if it makes the page
  sluggish

Build this with the same care as building a real internal tool — clean,
intentional visual design, not default browser styling. This is being
presented to 119 students as a reflection of engineering quality, so it
should look like it.

Cover the same 10 sections below, adapted to this interactive structure
rather than flat markdown prose.

### 1. Architecture Overview (plain-language, one page)

A short narrative explanation of how the whole system fits together —
written the way you'd explain it out loud to someone before showing any
code. Include a simple ASCII diagram of the request flow:
`Client → Routes → Middleware → Controller → Service → Model → Database`,
and a second diagram for the real-time path:
`Client (Socket.io) → sockets/ handler → same Service layer → DB → broadcast back`.

### 2. Folder-by-Folder Walkthrough

For every top-level and second-level folder in the actual repo, one entry:
- **Path**
- **Purpose** (one or two sentences)
- **What lives here and why it's separated from the folder next to it** — e.g.
  explicitly explain why `controllers/` and `services/` are different
  folders and not merged, since this is the #1 thing amateur builds get
  wrong and the #1 thing worth explaining well

### 3. Trace One Real Request, End to End

Pick the ticket status-change request (`PATCH /api/tickets/:id/status`) and
walk it through every file it touches, in order, with the actual code quoted
at each step:
1. The route definition
2. Every middleware it passes through (auth, permission check, validation) —
   what each one does and what happens if it fails
3. The controller — what it delegates vs. what it does itself
4. The service function — the actual business logic (state machine check)
5. The model/DB write
6. The side effects it triggers (ActivityLog entry, Notification creation,
   Socket.io emit)
7. The response — how it flows back through `utils/response.js`

This section should be detailed enough that the trainer could narrate it
line-by-line live without needing to re-derive anything.

### 4. Key Pattern Explanations

For each of the following, explain: what problem it solves, where it's
implemented (exact file path), and what would go wrong without it. Use a
concrete before/after or "what if we didn't do this" framing for each:
- Permission matrix middleware (`middlewares/permit.js`)
- Ticket state machine (`utils/ticketStateMachine.js`)
- Reusable response/error format (`utils/response.js` + `asyncHandler.js`)
- Service layer pattern (why controllers don't call the DB directly)
- React Context usage (`AuthContext`, `TicketContext`) — what prop-drilling
  problem it avoids, with a concrete example of what the code would look
  like without it
- Storage adapter pattern for file uploads
- Why chat is both persisted (ChatMessage) AND real-time (Socket.io) instead
  of just one or the other
- Centralized error-handling middleware and Winston logging — how an error
  actually flows from a thrown exception to a log entry to a client response

### 5. "Why Not the Obvious Way" — Function-Level Deep Dives

This is the most important section for the trainer personally, so give it
real depth, not a table of one-liners.

Scan the entire codebase for every function, method, or pattern that is NOT
the first/simplest/tutorial way a student would think to write it — anything
where a student coming from basic Express/React tutorials would look at the
code and think "wait, why didn't they just do X instead?" Examples of the
kind of thing to catch (do not limit yourself to these — actually scan the
real code for real instances):

- `http.createServer(app)` + Socket.io attach, instead of `app.listen()`
- Any place a Promise-based pattern is used instead of the "obvious"
  sequential await (e.g. `Promise.allSettled` instead of multiple awaits)
- Any middleware composition that isn't immediately self-explanatory
- Any Mongoose query using aggregation instead of a simpler `find()`, and why
  the simpler version wouldn't have worked or would have been slower
- Any place a factory/adapter pattern is used instead of calling something
  directly (e.g. the storage adapter for uploads)
- Any custom hook in the frontend that wraps something a student might
  expect to just call directly
- Any use of `next()` with an error vs. throwing directly
- Socket.io namespace/room logic that isn't a single `io.emit()`
- Any place middleware order matters and would break if reordered

For EVERY instance found, write it in this exact structure:

**`functionOrPatternName()`** — `path/to/file.js`, line ~N
- **What a student would expect / the "obvious" way**: [the naive version]
- **What this code actually does instead**: [quote the real code]
- **Why**: [the actual technical reason — what breaks or what's worse about
  the obvious way]
- **One-liner to say out loud to students**: [a single spoken sentence the
  trainer can literally say live, in the same style as: "app.listen() is
  Express doing this for you automatically. The moment you need something
  else — like Socket.io — to plug into the same server, you have to build
  that server yourself first, so both things can attach to it before it
  starts accepting connections."]

Do not skip anything because it seems minor — the goal is that after reading
this section, the trainer has zero code left in the repo that they couldn't
explain if a student pointed at it and asked "why is it written like that?"

### 6. File-by-File Cheat Sheet

A table covering every non-trivial file in `backend/src/` and `frontend/src/`:
| File | One-line purpose | What to point at when explaining it live |

Keep each "what to point at" cell genuinely actionable — e.g. not "explains
auth" but "show the token check on line X and ask a student what happens if
you delete that line."

### 7. Trainer Narration Script, Mapped to the Hour-wise Schedule

For each Hour block in the existing 2-day schedule (Hour 1–7, Day 1 and
Day 2), write a short scripted narration the trainer could read nearly
verbatim: what to say when introducing the topic, which file to have open,
what to run/click to demonstrate it, and a natural transition line into the
next hour. Assume the trainer is competent but seeing this specific code
explanation for the first time — write it as a real script, not a bullet
outline.

### 8. Anticipated Student Questions (Q&A bank)

For each major topic (auth, permission matrix, workflow state machine,
query optimization/aggregation, Socket.io chat, file upload, deployment),
list 3-5 questions students are realistically likely to ask, each with a
concise, confident answer grounded in this specific codebase — not a
textbook answer. Include at least one "trick" question per topic of the
kind an interviewer might ask (e.g. "why not just use PATCH for everything
instead of a dedicated status endpoint?").

### 9. Glossary

Every term/library/pattern name used in the codebase that a trainer might
get asked to define on the spot (JWT, middleware, aggregation pipeline,
compound index, WebSocket vs REST, service layer, DTO-like response
envelope, etc.) — one clear sentence each, in this project's context, not a
generic definition copy-pasted from documentation.

### 10. Live Demo Runbook

A literal step-by-step sequence: commands to run, in what order, to bring
the whole stack up from a clean clone and demonstrate it working — login as
each of the three roles, create a ticket, walk it through the workflow,
show chat, show an attachment upload, show a notification arrive in
real time, show the Postman collection run, show Swagger docs, show a test
suite pass. This is the "if everything else fails, here's exactly what to
click" section.

## STYLE

Write for someone who is competent and technical but is meeting this
specific codebase's decisions for the first time. Avoid restating basic
JavaScript/React concepts they already know; focus entirely on *this
repo's* specific choices and *why*. Use short paragraphs and code
references over long prose blocks. This document will be read under time
pressure before a live training session, so make it skimmable, not just
complete.

## OUTPUT LOCATION

Save as `TicketDesk_Trainer_Explainer.html` at the repository root. It
should open correctly by double-clicking it in a file browser — verify this
works before finishing (no relative paths that break outside a dev server,
no unresolved local imports).
