# Claude Session Start — Read This First

This file is the entry point for any new Claude session on this repository. Read it before touching any code.

---

## Project Summary

**`auth-service-java`** is a stateless JWT authentication REST API.
- **Stack:** Java 17, Spring Boot 3.2, Spring Security 6, JJWT 0.12, H2 (in-memory), Maven
- **Endpoints:** `POST /auth/signup`, `POST /auth/login` — both return `{token, tokenType}`
- **All other routes** require `Authorization: Bearer <token>` → 401 if missing/invalid

## Current Status (as of 2026-06-14)

**Phase 1 is complete.** All acceptance criteria met. 23 tests pass. `main` is green.

Latest merged PR: **#6 — fix: return 401 instead of 403 for unauthenticated requests**

## Current Milestone

None active. Phase 1 done. Next work is Phase 2 (see backlog).

## Current Priorities

1. **No critical bugs.** The codebase is stable.
2. **Phase 2 starting point** (choose one): PostgreSQL migration, CI/CD pipeline, or refresh tokens.
3. Check `docs/PROJECT_BACKLOG.md` for the full ranked list.

## Required Docs to Read

Before implementing anything, read in this order:

| File | Why |
|------|-----|
| `CLAUDE.md` | Coding rules, commands, architecture overview, phase scope |
| `docs/PROJECT_CONTEXT.md` | Full architecture, security model, testing strategy |
| `docs/PROJECT_PROGRESS.md` | What has been done, PR history, metrics |
| `docs/PROJECT_DECISIONS.md` | Why things are the way they are — read before changing anything |
| `docs/PROJECT_BACKLOG.md` | What's next, prioritised |
| `docs/spec.md` | Phase 1 API contract (still authoritative for existing endpoints) |

## Rules of Engagement

### What to do
- Follow the layered architecture: HTTP logic in controller, business logic in service, no exceptions.
- Keep DTOs for all request/response objects — never expose JPA entities.
- Write tests for every new feature: unit test for service logic, integration test for HTTP contract.
- Run `mvn test` before declaring anything done.
- Update `docs/PROJECT_PROGRESS.md` when a milestone completes.
- Add a `PROJECT_DECISIONS.md` entry for any significant design choice.

### What NOT to do
- Do not add Phase 2+ features (refresh tokens, roles, email verification, OAuth2) unless explicitly asked.
- Do not modify `AuthController`, `AuthService`, `JwtUtil`, DTOs, or entities for the 401/403 fix — those are in scope only for the security config.
- Do not expose JPA entities in API responses.
- Do not hardcode secrets — always use environment variables.
- Do not commit `.env` files (it's in `.gitignore`).
- Do not use `UserDetailsService` — Phase 1 bypasses it intentionally (see `docs/PROJECT_DECISIONS.md` D-009).

### Phase scope reminder
Phase 1 only: signup, login, JWT issuance/validation. Absent by design: refresh tokens, email OTP, RBAC, API keys, external providers, scheduled jobs.

## Git Safety Rules

- **Branch:** always create a feature branch off `main`, never commit directly to `main`.
- **Branch naming:** `claude/<short-description>-<id>` (follow existing pattern).
- **Commit messages:** imperative mood, concise. Follow the style in `git log --oneline`.
- **Push:** `git push -u origin <branch>` then open a **draft PR**.
- **Never** `--force-push` to `main`.
- **Never** `--no-verify` (skip hooks).
- **Never** amend a pushed commit — create a new one.
- **Confirm with the user** before merging or closing a PR.

## Quick Commands

```bash
mvn compile -DskipTests      # fast compile check
mvn test                      # run all 23 tests
mvn spring-boot:run           # start on :8080 (requires JWT_SECRET env var)
export JWT_SECRET="replace-with-32-plus-char-secret-here"
```

H2 console (dev profile only): `http://localhost:8080/h2-console`
JDBC URL: `jdbc:h2:mem:authdb`, user: `sa`, no password.
