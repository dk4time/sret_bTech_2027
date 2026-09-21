# Claude Prompt — Update the Trainer Explainer (Not a Rebuild)

Run this in Claude Code, inside the TicketDesk project folder.
`TicketDesk_Trainer_Explainer.html` already exists and works — **update it
in place**. Do not regenerate it from scratch and do not change anything
outside what's listed below. Everything else in the file (folder
walkthrough, request trace, cheat sheet, glossary, demo runbook) stays
exactly as it is.

---

## 0. PLACEMENT STRATEGY — DECIDE THIS FIRST

Before writing any content, decide, for each item below, whether it becomes
a **new navigable section/tab** in the existing sidebar, or an **addition
to a section that already covers this topic**. Do not default to "new tab"
for everything, and do not default to cramming everything into whatever
section is open — pick per item, using this rule:

> **New tab** = this is a genuinely new topic the current doc doesn't
> address anywhere yet, and it's substantial enough to deserve its own
> place in the sidebar navigation.
> **Enhance existing section** = the current doc already touches this
> topic somewhere (even briefly) — go find that exact spot and expand it
> in place, rather than creating a second, competing explanation elsewhere
> in the document.

Applying that rule to this update specifically:

- **Item 1 (Socket Handshake Deep-Dive) → NEW TAB.** This is genuinely new,
  foundational networking content the doc doesn't cover anywhere yet. Add
  it as its own entry in the sidebar navigation, positioned logically —
  directly after the existing "Key Pattern Explanations" section (or
  wherever Socket.io/real-time architecture is currently covered), since
  it's prerequisite knowledge that supports that section rather than a
  standalone unrelated topic. Give it its own sidebar label, something
  like "How Socket Connections Actually Work."
- **Item 2 (`io.js`/`chatSocket.js`/`server.js` explanation) → ENHANCE
  EXISTING SECTIONS, no new tab.** This is already partially covered in
  two places in the current doc: wherever the Folder-by-Folder Walkthrough
  describes the `sockets/` folder, and wherever Key Pattern Explanations
  discusses the chat/real-time pattern. Locate both spots and expand them
  in place with the fuller explanation — do not create a separate section
  that duplicates or competes with what's already there.
- **Item 3 (connection/room/message persistence) → ENHANCE EXISTING
  SECTION, no new tab.** This belongs directly alongside the "why chat is
  both persisted AND real-time" entry that should already exist in Key
  Pattern Explanations — add it there as a direct continuation of that
  explanation, not as a separate block elsewhere.

If, once you're actually looking at the real file, an item doesn't fit this
guidance cleanly (e.g. the current doc structure turns out to be organized
differently than assumed above), use the same new-tab-vs-enhance rule to
make the call yourself, and briefly note in your summary at the end which
choice you made and why.

## 1. ADD: Socket Handshake Deep-Dive (new tab — see section 0)

Add a new subsection under the real-time architecture content (near where
Socket.io is already explained) covering how a socket connection actually
gets established underneath TicketDesk's `sockets/io.js`. Include, in this
order:

- **TCP three-way handshake**: SYN → SYN-ACK → ACK, with a simple visual
  diagram (client/server columns, arrows between them — style it to match
  the existing diagram treatment already used elsewhere in the doc, e.g.
  the request-trace flow diagram)
- **The phone-call analogy** ("Hello?" / "Hello, I can hear you." / "Great,
  let's talk.") as a plain-language explainer alongside the technical
  diagram
- **Four-way termination handshake** (FIN/ACK/FIN/ACK) — brief, one diagram
- **UDP contrast** — no handshake, faster but unreliable, why it's used for
  video/voice/gaming instead — kept short, this is context, not the focus
- **WebSocket handshake specifically** — HTTP GET with `Upgrade: websocket`
  header → server responds `101 Switching Protocols` → connection upgrades
  from HTTP to a persistent full-duplex connection. Make this the most
  detailed part since it's what actually happens in this project.
- **TCP vs TLS handshake** — a small layered diagram (IP → TCP handshake →
  TLS handshake → application data), one or two sentences on what each
  layer negotiates
- **Explicit tie-back to the actual codebase**: end this subsection with a
  short paragraph connecting it directly to `sockets/io.js` — something
  like "this is the exchange happening before your `connection` event
  handler ever fires" — so it doesn't read as generic networking theory
  disconnected from the project

## 2. UPDATE: `io.js` / `chatSocket.js` / `server.js` explanation (enhance existing sections — see section 0)

Find wherever the current explainer covers the sockets folder and rewrite
it to be more complete. First, **read the actual files in this repo** —
`backend/sockets/io.js`, `backend/sockets/chatSocket.js`, and how
`server.js` wires them together — and base the explanation on the real
function/variable names in this codebase, not generic examples. Cover:

- `io.js`'s responsibility: creating and configuring the Socket.io
  instance as a singleton (`init()` / `getIO()` pattern or whatever this
  repo actually uses), so it's created exactly once and made available
  everywhere else that needs to emit events
- `chatSocket.js`'s responsibility: receiving the `io` instance and
  registering event handlers (connection, join-room, send-message,
  disconnect) — business logic, not setup
- How `server.js` ties them together — show the actual flow:
  `server.js → creates HTTP server → io.js creates Socket.io instance →
  chatSocket.js registers events → clients connect`, as a small diagram
  matching the visual style already used for the request-trace flow
- **Why they're separate files**: if this project grows more real-time
  features (notifications, live tracking, etc.), each gets its own file
  registering its own events against the same shared `io` instance, instead
  of one growing file handling everything. Use the classroom analogy:
  "`io.js` builds the classroom and opens the door; `chatSocket.js`
  runs the chat session inside it; a `notificationSocket.js` would handle
  announcements the same way — the classroom itself is built once, and
  different modules use it for different things"
- If this repo already has a `notificationSocket.js`, reference it directly
  as the real second example, rather than a hypothetical

## 3. UPDATE: Connection / room / message persistence explainer (enhance existing section — see section 0)

Add (or replace, if something related already exists) a clear explanation
of what persists at what scope — this should be presented as three short,
clearly separated points, not one paragraph:
- **Connection** — persists for the session: connects at login, disconnects
  at logout or tab close
- **Room membership** — persists only per ticket view: joined on opening a
  ticket's detail page, left on navigating away; shorter-lived than the
  connection itself
- **Messages** — persist permanently in the `ChatMessage` collection,
  completely independent of login state

Place this wherever the chat/real-time explanation already lives in the
doc, right after the `io.js`/`chatSocket.js` section from item 2.

## 4. UX FIX: Convert ALL Q&A content to click-to-reveal

This applies sitewide, not just to one section. Anywhere the document shows
a question followed immediately by a visible answer, change it to:
question displayed, with a **"Reveal Answer"** button beneath it; answer
stays hidden until clicked, then appears (reuse whatever reveal/expand
animation and button styling is already established elsewhere in the doc —
e.g. the click-to-expand behavior in the "Why Not the Obvious Way" section
— for visual consistency, don't introduce a new interaction style).

Apply this to:
- The existing "Anticipated Student Questions (Q&A Bank)" section — every
  entry, no exceptions
- Any new Q&A-style content added in this update, including the socket
  handshake section if you include a check-style question there (e.g. "What
  stops a Requester from opening a raw socket to a ticket they don't own?"
  → Reveal Answer → "This file re-verifies the JWT on every connect;
  `chatSocket.js` re-checks room permission on every join.")

## 5. UX FIX: Rename and reframe "Trainer Narration Script" section

Current heading: "Trainer Narration Script — Mapped to the Schedule."
Problem: this document sometimes gets projected in front of students, and
a visible heading that says "script" makes it look like the trainer is
reading from notes rather than teaching.

Fix it two ways:

- **Rename the heading** to something that reads as a roadmap or flow, not
  a script — e.g. "Session Flow — Hour by Hour" or "How We'll Build This"
  (pick whichever fits the doc's existing tone; keep it short)
- **Restructure the content itself into two layers**, so it works for both
  audiences at once:
  - **Default visible view**: a clean list of what happens each hour — the
    topic, the file being worked on, what gets demonstrated — written like
    a milestone list a student would find genuinely useful to see, not
    trainer notes
  - **Hidden-by-default, revealable per hour**: the literal spoken cue
    lines from the original narration script, behind a small "Trainer cue"
    toggle/reveal — same reveal pattern as item 4 — so the trainer can
    still access their talking points privately without students seeing
    "here is what I am about to say to you" on the projector

## 6. CONSISTENCY CHECK

Before finishing, verify the new content matches the existing document's
visual language exactly — same fonts, same color coding for diagrams
(reuse whatever colors are already used for flow diagrams elsewhere), same
reveal/expand button styling, same spacing rhythm. This should read as one
coherent document, not new content bolted onto an old one.

## OUTPUT

Update `TicketDesk_Trainer_Explainer.html` in place. Do not create a new
file or rename the existing one.
