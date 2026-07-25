---
name: resume
description: Start of session — read HANDOFF.md and the docs it points to, verify them against real git/test state, and produce a short orientation so work can continue without re-deriving context. The inverse of /handoff.
tags: [session, handoff, continuity, orientation]
---

# Resume Skill

Orient a fresh session from the handoff so you can **continue, not restart**. This is the read/verify
counterpart to `/handoff` (which writes the handoff). Do this before doing any new work.

## When to Run

- At the **start** of a session that continues earlier work.
- Any time the user types `/resume` (or asks to "continue", "catch up", "pick up where we left off").

## Procedure

Do these in order. **Trust the handoff for intent, but verify state against reality before acting.**

1. **Read, in this order:**
   - `CLAUDE.md` — project rules + reading order.
   - `docs/HANDOFF.md` — read the **top authoritative sections** and the **New Session Prompt at the
     bottom** (ignore sections explicitly marked ⚠️ superseded).
   - Whatever the handoff's "read first" / "exact next step" points to (e.g. a plan doc like
     `docs/PORTAL_MILESTONE_1.md`, or a specific ADR).

2. **Verify the handoff against real state (don't trust stale claims):**
   - `git status --short`, `git branch -vv`, `git log --oneline -5` → real branch, uncommitted files,
     last commit. Note if the session's work is committed or still in the working tree.
   - Test claim: check whether the handoff's test count matches reality. If source/tests changed since
     the handoff's stated run, **re-run** (this project: `JAVA_HOME` = IntelliJ JBR 21 + bundled Maven;
     not system JDK 24). If unchanged, trust the stated result but say it wasn't re-run.
   - **Flag any drift** between what HANDOFF.md claims and what git/tests show (e.g. "handoff says
     committed but working tree is dirty", "test count differs").

3. **Produce a short orientation (≤ ~12 lines):**
   - **Current goal** (from handoff).
   - **State**: branch, committed vs uncommitted, test status (verified).
   - **Done — do NOT redo** (completed work + known backend gaps that must not be "fixed" by inventing
     features).
   - **Exact next step** (the concrete task to start).
   - **Any drift/risks** you found in step 2.

4. **Confirm and proceed.** State the next action from the handoff. If it's unambiguous and low-risk,
   start it. If there's drift, a decision point (e.g. "commit first?"), or ambiguity, ask the user
   before doing new work.

## Rules

- **Verify over trust** — the handoff is a point-in-time note; git and a real test run are ground truth.
- **Respect scope constraints** recorded in the handoff (e.g. "ignore AI Guardline", "don't widen
  backend scope", "don't invent missing endpoints").
- **Don't redo finished work** — the whole point is continuity.
- **Don't commit** unless asked.
- If `docs/HANDOFF.md` is missing or clearly stale, say so and reconstruct intent from git log + docs
  rather than guessing.

## References

- [docs/HANDOFF.md](../../docs/HANDOFF.md) — the session log this skill reads
- [.claude/skills/handoff/SKILL.md](../handoff/SKILL.md) — the counterpart that writes the handoff
- [CLAUDE.md](../../CLAUDE.md) — project rules + reading order
