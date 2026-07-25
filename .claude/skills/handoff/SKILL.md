---
name: handoff
description: Update HANDOFF.md as a living session log and write a ready-to-paste next-session prompt before ending a session. Run at the end of any planning or implementation session.
tags: [session, handoff, docs, continuity]
---

# Handoff Skill

Produce a precise, current handoff so a **fresh Claude session can continue without guessing**.
`docs/HANDOFF.md` is a **living session log**, not a generic summary. Run this before stopping work.

## When to Run

- At the end of **any** planning or implementation session (even if the session only planned).
- Whenever the project's reality changed (code, tests, config, docs, decisions).
- Any time the user types `/handoff`.

## Procedure

Do these in order. Prefer facts you can verify **now** over memory.

1. **Gather current state (verify, don't guess):**
   - `git status --short` and `git diff --stat` → the real list of created/modified files.
   - `git log --oneline -8` → recent commits.
   - Test result: report the **actual** last run. If tests were touched this session and you don't
     have a fresh result, run them (this project: set `JAVA_HOME` to the IntelliJ JBR 21 and use the
     bundled Maven — see the build note in `docs/HANDOFF.md`; do **not** use system JDK 24). If you
     genuinely can't run them, say so and give the last known result with its date.

2. **Update `docs/HANDOFF.md`** so its top is authoritative. It must contain, clearly:
   - **Current goal** — one or two lines.
   - **What was completed this session** — specific changes, not vague summary.
   - **Files created/modified** — from git, grouped (source / tests / docs / config).
   - **Tests run and results** — counts + pass/fail + how they were run.
   - **Key decisions made** — and the *why* in a clause each.
   - **Known constraints / backend gaps** — what does NOT exist (so the next session won't invent it).
   - **Open risks / TODOs** — including anything intentionally deferred and why.
   - **Exact next step** — concrete enough to start immediately (name the file/task).

3. **Update any docs that this session made stale** — e.g. `docs/PRD.md`, `docs/PROJECT_BACKLOG.md`,
   `docs/ADR/*`, README. Only if project reality actually changed. If a section of HANDOFF is now
   historical, mark it `superseded` rather than deleting it. If nothing became stale, say so in the
   handoff (don't touch docs needlessly).

4. **Write a ready-to-paste "New Session Prompt" at the very bottom of `docs/HANDOFF.md`** telling the
   next session:
   - **Read first** — the exact files, in order (CLAUDE.md, HANDOFF.md top, the relevant plan/ADR).
   - **Current state** — the authoritative one-paragraph status (branch, tests, what's done).
   - **What NOT to redo** — the completed work + the known gaps it must not "fix" by inventing features.
   - **What to do next** — the exact next task.

5. **Keep it concise but specific.** A fresh session should be able to start in minutes. Trim or mark
   stale content; don't let the file grow into an unreadable pile.

6. **Report back** to the user: a short bullet list of what you updated (files) and confirm the New
   Session Prompt is in place. Note if changes are uncommitted (this project commits only when asked).

## HANDOFF.md Skeleton

```markdown
# Session Handoff

**Last updated**: <date> (<short session title>)
**Branch**: `<branch>`
**Tests**: <N passing / M failing> — <how run>
**Current goal**: <1–2 lines>

---

## What Was Completed This Session (<date> — <title>)
- <specific change> …

### Files created/modified
| File | Change |
|------|--------|
| … | … |

### Tests run and results
<counts, pass/fail, command used>

### Key decisions
- <decision> — <why>

### Known constraints / backend gaps (do NOT invent these)
- <gap> …

### Open risks / TODOs
- <risk / deferred item + why>

### Exact next step
<name the file/task to start with>

---

<!-- older sessions below; mark superseded content with a ⚠️ note -->

## 🔖 New Session Prompt (ready to paste)

> <one-paragraph current state>. Read first: <files, in order>. Do NOT redo: <completed work + gaps>.
> Next: <exact task>. Constraints: <hard scope limits>. Run `/handoff` before stopping.
```

## Rules

- **Verify over recall** — pull files/tests from git and real runs, not memory.
- **Never invent backend features** — list what's missing as a gap; the next session must not "fill" it.
- **Living log** — update the top to current truth; mark old content superseded, don't silently drop it.
- **Respect project scope constraints** captured in the session (e.g. "ignore AI Guardline").
- **Don't commit** unless the user asked — just report that changes are uncommitted.

## References

- [docs/HANDOFF.md](../../docs/HANDOFF.md) — the file this skill maintains
- [CLAUDE.md](../../CLAUDE.md) — session start + reading order (rule 8: update HANDOFF before stopping)
