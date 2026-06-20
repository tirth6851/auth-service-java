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

### Milestone 5 — 401 Fix (PR #6)
**Commit:** `8140b4e fix: return 401 instead of 403 for unauthenticated requests`
**Merged:** 2026-06-14
- **Root cause fixed:** Spring Security 6 defaulted to `Http403ForbiddenEntryPoint`; unauthenticated requests incorrectly returned 403.
- **New file:** `Http401UnauthorizedEntryPoint` implements `AuthenticationEntryPoint`, sends 401.
- **SecurityConfig** wired with `.exceptionHandling(eh -> eh.authenticationEntryPoint(...))`.
- **Tests updated:** two existing 403 assertions → 401; new `protectedRoute_passesAuth_whenValidToken` test added.
- **Test infrastructure:** `src/test/resources/application.properties` created so integration tests run without `JWT_SECRET` env var.
- Total tests: **23** (was 17 before Milestone 4 expansions + this PR added 3 net new).

### Milestone 6–8 — Docker, PostgreSQL, Actuator (Sessions since M5)
- Multi-stage Dockerfile + docker-compose.yml with PostgreSQL 16
- Flyway V1 migration (`V1__create_users_table.sql`, LOWER(email) index)
- `/actuator/health` endpoint enabled; `.env.example` + `.dockerignore` added
- PostgreSQL datasource, `application-prod.properties`, H2 for dev/test unchanged

### Milestone 9 — Security Hardening Round 2 (PR #9, merged 2026-06-19)
**Branch**: `claude/security-hardening-round2`
- `Dockerfile` — non-root `spring` user in runtime stage (least-privilege)
- `SecurityConfig` — `frameOptions.disable()` → `frameOptions.sameOrigin()` (clickjacking protection)
- `AuthControllerIntegrationTest` — added `responses_includeXFrameOptionsSameOrigin()` test
- `docker-compose.yml` — removed demo `JWT_SECRET` fallback that bypassed JwtUtil validation
- `.env.example` — demo JWT_SECRET replaced with JwtUtil-rejected placeholder (closes onboarding-path gap)
- `.github/workflows/ci.yml` — `upload-artifact@v3` → `@v4` (action deprecation fix)
- `CLAUDE_SESSION_START.md` — corrected `APP_JWT_SECRET` → `JWT_SECRET`
- `docs/RUNBOOK.md`, `docs/ARCHITECTURE.md` — updated to match changes

### Milestone 10 — Refresh Tokens, Logout, CORS, Actuator Security (2026-06-19)
**Branch**: `claude/refresh-tokens`
- **Refresh tokens**: UUID v4 stored as SHA-256 hash; 7-day TTL; rotation on every refresh
- **`POST /auth/refresh`**: validates token (not expired, not revoked), revokes old, issues new pair
- **`POST /auth/logout`**: revokes refresh token; idempotent on already-revoked
- **`POST /auth/signup`** and **`POST /auth/login`** now return `refreshToken` in response
- **CORS**: `CorsConfigurationSource` bean in `SecurityConfig`; origins configured via `app.cors.allowed-origins`
- **`/actuator/health`**: added to `permitAll()` — accessible without auth
- **V2 Flyway migration** (`V2__create_refresh_tokens_table.sql`): `refresh_tokens` table for PostgreSQL prod
- **10 new integration tests**: refresh success/expired/revoked/invalid, logout success/invalid, actuator health, CORS headers
- **`AuthServiceTest`** updated to mock `RefreshTokenService`
- **ADR-005**: refresh token design rationale (hashed UUID, rotation, revocation)
- **ADR-006**: MCP/agent-auth architecture note

### Milestone 11 — Open-Source Readiness + Developer Experience (2026-06-19)
**Branch**: `claude/refresh-tokens` (continuing same branch)
- **OpenAPI / Swagger UI**: `springdoc-openapi-starter-webmvc-ui:2.5.0`; UI at `/swagger-ui.html`; disabled in prod
- **`OpenApiConfig.java`**: `@OpenAPIDefinition` (title, description) + `@SecurityScheme` (bearerAuth / JWT)
- **`AuthController`** fully annotated: `@Tag`, `@Operation`, `@ApiResponses` on all 4 endpoints
- **All DTOs** annotated with `@Schema` (descriptions, examples): `SignupRequest`, `LoginRequest`, `AuthResponse`, `RefreshRequest`, `LogoutRequest`, `ErrorResponse`
- **`SecurityConfig`**: `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` added to `permitAll()`; `sameOrigin()` preserved from M9
- **`application-prod.properties`**: `springdoc.api-docs.enabled=false`, `springdoc.swagger-ui.enabled=false`
- **`CONTRIBUTING.md`**: branch naming, coding rules, test expectations, ADR process, security contact
- **`CODE_OF_CONDUCT.md`**: Contributor Covenant v2.1
- **`.github/ISSUE_TEMPLATE/bug_report.md`** and **`feature_request.md`**: structured issue templates
- **`.github/PULL_REQUEST_TEMPLATE.md`**: security checklist, docs-update checklist, breaking-change declaration
- **`docs/DEPLOYMENT.md`**: environment variables table, profiles guide, Docker deployment, JAR deployment, cloud platform notes, production checklist, rollback guidance
- **`docs/OBSERVABILITY.md`**: logging baseline (recommended events, not yet implemented), monitoring signals, future JSON logging setup, audit event table
- **`docs/ADR/006`** extended: "Where to Start" guide for MCP contributors, suggested MCP tool names
- **`README.md`**: updated badges (CI now configured), Swagger quick-start step, interactive docs link, accurate file-structure tree, accurate test inventory, updated roadmap (completed items marked ✅, priority table for next sprint)
- **`AuthControllerIntegrationTest`**: +3 tests: `responses_includeXFrameOptionsSameOrigin`, `openApiDocs_returns200_withoutAuth`, `swaggerUi_isReachable`
- **40 tests — all passing** (includes security hardening + Swagger tests)

## Latest Metrics (2026-06-19)

| Metric | Value |
|--------|-------|
| Test count | 40 (0 failures) |
| Build status | `mvn test` — BUILD SUCCESS |
| Branch | `claude/refresh-tokens` |
| Open PRs | 1 (refresh tokens + open-source readiness sprint) |
| Phase | 3 — Open-source readiness complete |

## Recent PR History

| PR | Title | Status |
|----|-------|--------|
| TBD | feat: refresh tokens, logout, CORS, actuator auth + open-source readiness | Open — `claude/refresh-tokens` |
| #9 | security: harden Dockerfile, frame options, and docker-compose secret handling | Merged 2026-06-19 |
| #6 | fix: return 401 instead of 403 for unauthenticated requests | Merged 2026-06-14 |
| #4 | security: harden JWT guard, .env protection, and missing test coverage | Merged |
| #3 | config: separate H2 console setting by Spring profile | Merged |
| #2 | docs: comprehensive README audit, roadmap, and next-steps guide | Merged |
| #1 | Phase 1: Spring Boot authentication API | Merged |

## Branch Status

- `main` — Phase 1 + security hardening, 24 tests
- `claude/refresh-tokens` — Phase 2 + 3 feature branch, 40 tests, PR pending
- `claude/rate-limit-login` — Rate limiting branch, separate PR pending
