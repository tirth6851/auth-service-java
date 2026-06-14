# Project Progress

## Current Status

**Phase 1 — Complete and stable.**
All acceptance criteria met. 23 tests pass. `main` branch is green.

## Milestone History

### Milestone 1 — Phase 1 Implementation (PR #1)
**Commit:** `8f344c7 Phase 1: Spring Boot authentication API`
- Spring Boot project scaffolded
- `User` entity, `UserRepository`, `AuthService` (signup + login)
- `JwtUtil` (HS256, JJWT 0.12), `JwtAuthenticationFilter`, `SecurityConfig`
- `SignupRequest`, `LoginRequest`, `AuthResponse` DTOs with Bean Validation
- `AuthController` — POST /auth/signup, POST /auth/login
- `application.properties`, `CLAUDE.md`, initial README

### Milestone 2 — Test Coverage (PRs #1 + standalone commits)
**Commits:** `f9adabe` → `19c2773`
- `JwtUtilTest` — 5 unit tests (later expanded to 8)
- `AuthServiceTest` — 5 unit tests with Mockito
- `AuthControllerIntegrationTest` — 7 integration tests (later expanded to 10)
- Byte Buddy experimental mode enabled for Java 25 Mockito compatibility

### Milestone 3 — Docs + README Audit (PR #2)
**Commits:** `5a73b73`, `77c7158`
- Comprehensive README with run instructions, endpoints, curl examples
- `docs/spec.md`, `docs/todo.md`, `docs/done-criteria.md`
- `docs/superpowers/plans/` — agentic implementation plans

### Milestone 4 — Security Hardening (PRs #3 + #4)
**Commits:** `b439863`, `c5a72fb`, `070c296`, `4c2919e`
- `/error` added to permit-all so `ResponseStatusException` status codes reach clients
- H2 console split by Spring profile (disabled in default, enabled in `dev`)
- JWT secret externalized to `JWT_SECRET` env var (fail-fast validation in `JwtUtil.init()`)
- `.env` added to `.gitignore`
- JwtUtil validation tests strengthened (min key length, blank secret)
- Integration test `protectedRoute_isDenied_whenNoToken` and `_whenInvalidToken` added

### Milestone 5 — 401 Fix (PR #6) — **Latest**
**Commit:** `8140b4e fix: return 401 instead of 403 for unauthenticated requests`
**Merged:** 2026-06-14
- **Root cause fixed:** Spring Security 6 defaulted to `Http403ForbiddenEntryPoint`; unauthenticated requests incorrectly returned 403.
- **New file:** `Http401UnauthorizedEntryPoint` implements `AuthenticationEntryPoint`, sends 401.
- **SecurityConfig** wired with `.exceptionHandling(eh -> eh.authenticationEntryPoint(...))`.
- **Tests updated:** two existing 403 assertions → 401; new `protectedRoute_passesAuth_whenValidToken` test added.
- **Test infrastructure:** `src/test/resources/application.properties` created so integration tests run without `JWT_SECRET` env var.
- Total tests: **23** (was 17 before Milestone 4 expansions + this PR added 3 net new).

## Latest Metrics (2026-06-14)

| Metric | Value |
|--------|-------|
| Test count | 23 (0 failures) |
| Build status | `mvn test` — BUILD SUCCESS |
| Branch | `main` (PR #6 merged) |
| Open PRs | 0 |
| Phase | 1 — Complete |

## Recent PR History

| PR | Title | Status |
|----|-------|--------|
| #6 | fix: return 401 instead of 403 for unauthenticated requests | Merged 2026-06-14 |
| #4 | security: harden JWT guard, .env protection, and missing test coverage | Merged |
| #3 | config: separate H2 console setting by Spring profile | Merged |
| #2 | docs: comprehensive README audit, roadmap, and next-steps guide | Merged |
| #1 | Phase 1: Spring Boot authentication API | Merged |

## Branch Status

- `main` — fully up to date, all Phase 1 work merged
- No open feature branches
