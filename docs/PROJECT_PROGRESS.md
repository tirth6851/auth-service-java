# Project Progress

## Current Status

**Phase 1 — Complete and stable. Phase 2/3 features (refresh tokens, CORS, OpenAPI, /auth/me, rate limiting) merged to `main`.**
See "Latest Metrics" below for current test count. `main` branch is green.

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

### Milestone 12 — GET /auth/me (2026-06-19)
**Branch**: `claude/auth-me`
- `GET /auth/me` — protected endpoint returning id, email, verified, createdAt
- `MeResponse` DTO (record) — no entity exposure
- `AuthService.getCurrentUser(email)` — DB lookup; 401 if user not found
- `SecurityConfig` — explicit `permitAll` list (`/auth/signup`, `/auth/login`, `/auth/refresh`, `/auth/logout`, plus health/swagger paths); `/auth/me` requires auth
- `application.properties` — ISO 8601 date serialization enabled
- `AuthControllerIntegrationTest` — 3 new tests (valid token → 200, no token → 401, invalid token → 401)
- `docs/API_CONTRACT.md` — GET /auth/me endpoint spec added

### Milestone 13 — Rate Limiting on /auth/login (2026-06-19)
**Branch**: `claude/rate-limit-login` (stacked on `claude/auth-me`)
- `pom.xml` — added `com.bucket4j:bucket4j-core:8.10.1`
- `LoginRateLimitInterceptor` — HandlerInterceptor; per-IP token bucket (10/10min); throws `RateLimitExceededException`
- `WebConfig` — registers interceptor for `/auth/login` only
- `RateLimitExceededException` — carries `retryAfterSeconds` for header
- `GlobalExceptionHandler` — `handleRateLimit()` returns 429 + `Retry-After` header + `ErrorResponse`
- `application.properties` — rate limit defaults configurable via env (`app.ratelimit.login.*`)
- `test/application.properties` — capacity overridden to 3 for fast integration tests
- `AuthControllerIntegrationTest` — 3 new tests (under limit → 401, over limit → 429, 429 header + body)
- `docs/API_CONTRACT.md` — rate limiting section updated with policy, headers, config
- `docs/ADR/007-rate-limiting-strategy.md` — design rationale (library, keying, placement, refill strategy); renumbered from 005 to avoid collision with the refresh-token ADR

### Milestone 14 — Merge consolidation: auth-me + rate-limiting into main (2026-06-30)
- Merged `claude/rate-limit-login` (which already contained `claude/auth-me`) into `main` after PR #10
- Resolved conflicts in `SecurityConfig` (kept explicit permitAll list so `/auth/me` stays protected), `AuthController` (all 5 endpoints retained), `AuthService` (auto-merged cleanly), `application.properties` (dev + test, all settings retained)
- Renumbered `docs/ADR/005-rate-limiting-strategy.md` → `007-rate-limiting-strategy.md`
- Closed stale PR #5 (401 fix already merged via PR #6)

## Latest Metrics (2026-06-30)

| Metric | Value |
|--------|-------|
| Test count | 46 (0 failures) |
| Build status | `mvn test` — BUILD SUCCESS |
| Branch | `main` |
| Open PRs | 0 |
| Phase | 3 — refresh tokens, CORS, OpenAPI, /auth/me, rate limiting all merged |

## Recent PR History

| PR | Title | Status |
|----|-------|--------|
| #10 | feat: refresh tokens, logout, CORS, and actuator security | Merged 2026-06-30 |
| #9 | security: harden Dockerfile, frame options, and docker-compose secret handling | Merged 2026-06-19 |
| #6 | fix: return 401 instead of 403 for unauthenticated requests | Merged 2026-06-14 |
| #5 | fix: return 401 Unauthorized for unauthenticated requests | Closed 2026-06-30 (superseded by #6) |
| #4 | security: harden JWT guard, .env protection, and missing test coverage | Merged |
| #3 | config: separate H2 console setting by Spring profile | Merged |
| #2 | docs: comprehensive README audit, roadmap, and next-steps guide | Merged |
| #1 | Phase 1: Spring Boot authentication API | Merged |

## Branch Status

- `main` — Phase 1 + PostgreSQL/Docker/CI + security hardening + refresh tokens/CORS/OpenAPI + /auth/me + rate limiting, all merged
- `claude/auth-me`, `claude/rate-limit-login` — merged into `main`; safe to delete
