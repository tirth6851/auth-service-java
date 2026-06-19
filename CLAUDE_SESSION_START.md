# Claude Session Start

**Read this file first, every session.** It is your entry point to the repository and the handoff from the previous session.

---

## Session Entry Checklist

**Before touching any code, complete this in order:**

1. ✅ **Read CLAUDE.md** (governance + quick rules)
2. ✅ **Read docs/HANDOFF.md** ← *previous session state & next steps* (see [HANDOFF Template](docs/HANDOFF.md))
3. ✅ **Assess task risk** (use MISSION_CONTROL.md decision matrix to determine scope)
4. ✅ **Read required docs** per decision authority (see below)
5. ✅ **Verify git status** (`git status`, `git log --oneline -5`)
6. ✅ **Run tests** (`mvn test` must pass)
7. ✅ **Start work** (follow code change workflow in MISSION_CONTROL.md)
8. ✅ **Update HANDOFF.md before stopping** ← *MANDATORY end-of-session rule*

---

## Required Reading Order (By Task Type)

### For ANY code change:
1. `CLAUDE.md` (5 min) — rules, quick start
2. `docs/HANDOFF.md` (2 min) — previous session state
3. `docs/MISSION_CONTROL.md` (5 min) — decision matrix, risk assessment
4. `docs/PRD.md` (5 min) — scope check (is feature in Phase 1?)
5. `docs/TRD.md` (5 min) — constraints, requirements

### For feature implementation (medium/high risk):
6. `docs/ARCHITECTURE.md` (10 min) — which layers are affected?
7. `docs/ENGINEERING_STANDARDS.md` (5 min) — coding rules, forbidden patterns
8. `docs/API_CONTRACT.md` (5 min) — endpoint specs (if API change)

### For bug fix or refactor (low risk):
6. `docs/ENGINEERING_STANDARDS.md` (5 min) — quick rules
7. Relevant domain docs (ARCHITECTURE or TEST_STRATEGY, as needed)

### Before release/merge:
6. `docs/TEST_STRATEGY.md` (5 min) — coverage requirements
7. `docs/ENVIRONMENTS.md` (5 min) — config check
8. `docs/RUNBOOK.md` (5 min) — deployment safety

### For context (always helpful):
- `docs/PROJECT_CONTEXT.md` — project mission, users, personas
- `docs/PROJECT_PROGRESS.md` — what's been done, metrics
- `docs/PROJECT_DECISIONS.md` — why decisions were made
- `docs/PROJECT_BACKLOG.md` — pending work, priorities
- `docs/ADR/` — major architectural decisions

---

## Current Project State

**Phase 1 Status**: Complete  
**Main branch**: Clean, all tests passing  
**Latest work**: [Check docs/HANDOFF.md for most recent session]  
**No active blockers**: [Update HANDOFF.md if you discover any]

---

## Non-Negotiable Rules (Zero Tolerance)

❌ **NEVER**:
- Code before reading required docs
- Put business logic in controllers (goes in service)
- Expose JPA entities in HTTP responses (use DTOs)
- Hardcode secrets (use environment variables)
- Skip tests (80%+ coverage required)
- Commit without updating HANDOFF.md at session end

✅ **ALWAYS**:
- Update docs in same commit as code changes
- Run tests: `mvn test` (must pass)
- Use `/security-review` if touching auth/config/errors
- Update HANDOFF.md before ending session
- Read MISSION_CONTROL.md for decision authority

---

## Quick Reference

### Commands
```bash
mvn test                          # All tests (must pass)
mvn clean compile -DskipTests     # Quick compile check
mvn spring-boot:run               # Run on localhost:8080
mvn jacoco:report                 # Coverage report
git status                        # Check for uncommitted changes
```

### Environment
```bash
export JWT_SECRET="demo-secret-minimum-32-characters-long-12345"
# (Never use demo secret in production)
```

### API
- `POST /auth/signup` — register user, returns JWT
- `POST /auth/login` — login, returns JWT
- Protected routes require: `Authorization: Bearer <token>`

### Stack
- **Language**: Java 17
- **Framework**: Spring Boot 3.2
- **Auth**: Spring Security + JJWT
- **Database**: H2 in-memory (dev), PostgreSQL (planned for prod)
- **Build**: Maven 3.6+
- **Testing**: JUnit 5, Mockito, Spring Test

---

## HANDOFF Protocol (Session Continuity)

At **the end of every work session**, you MUST update `docs/HANDOFF.md`:

- What you completed
- What's in progress
- Outstanding risks or blockers
- Exact next steps for the next session
- Files changed, tests run, docs updated

A session is **NOT complete** until HANDOFF.md is refreshed.

**Next session**: Read HANDOFF.md immediately after CLAUDE.md to understand where the previous session left off.

---

## Skills Available

- `/spring-auth-feature` — implement features following layered rules
- `/java-test-first` — plan tests, ensure coverage
- `/security-review` — audit secrets, auth, errors
- `/release-checklist` — pre-merge verification

Use them liberally. They enforce consistency and catch issues early.

---

## What to Do If Stuck

1. Check MISSION_CONTROL.md (red flags, decision matrix, escalation)
2. Check docs/PROJECT_DECISIONS.md (why things are the way they are)
3. Check HANDOFF.md (previous session may have hit same blocker)
4. Run `/security-review` or `/release-checklist` for mechanical verification
5. Re-read relevant domain doc (ARCHITECTURE, API_CONTRACT, etc.)
6. If still stuck: document the blocker in HANDOFF.md, ask for guidance

---

## Commit & PR Safety

- **Branch naming**: `claude/<description>-<id>` (follow existing pattern)
- **Commit message**: imperative, clear, under 50 chars
- **Push & PR**: `git push -u origin <branch>` → open draft PR
- **Before merge**: Run `/release-checklist`, get code review approval
- **Never**: force-push to main, skip hooks, amend published commits
- **Always**: Confirm with user before closing PR or merging

---

## Verification Before Stopping Work

- [ ] All tests pass (`mvn test`)
- [ ] No uncommitted changes (`git status` is clean)
- [ ] Relevant docs updated (ARCHITECTURE, API_CONTRACT, etc.)
- [ ] HANDOFF.md refreshed with next steps
- [ ] Branch pushed to remote (`git push`)
- [ ] PR created (if feature/fix)

Only after all boxes checked: **session complete**.
