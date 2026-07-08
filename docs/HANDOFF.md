# Session Handoff

**Last updated**: 2026-07-08 (Auth hardening + Portal M1 planning + `/handoff` skill)
**Branch**: `claude/refresh-tokens` (off `main`)
**Tests**: 62 passing (0 failures) — clean run this session via IntelliJ Maven + JBR 21; unchanged since (no test sources touched after that run)
**Current goal**: backend hardening is done + green; a **web auth portal (Milestone 1)** is now PLANNED
(not built) — full implementation plan in **`docs/PORTAL_MILESTONE_1.md`**. Next step is to scaffold
that portal in a new `web/` project. No backend code changed in the planning turns.
**Working tree: ALL of this session's changes are UNCOMMITTED** (see file list below). Commit only when asked.

> Build note: no `mvnw` wrapper and `mvn` is not on PATH. Build with IntelliJ's bundled Maven +
> JBR 21 (project targets Java 17; local system JDK is 24, which is too new for the ByteBuddy/Mockito
> stack). Commands used this session:
> ```
> export JAVA_HOME="/c/Program Files/JetBrains/IntelliJ IDEA 2025.3.1.1/jbr"
> "/c/Program Files/JetBrains/IntelliJ IDEA 2025.3.1.1/plugins/maven/lib/maven3/bin/mvn" -B test
> ```

---

## What Was Completed This Session (2026-07-08 — Auth correctness & security hardening)

Fixed the highest-priority auth correctness/security gaps and closed their test coverage. Net: 40 → 62 tests.

### Security / correctness fixes
- **Refresh-token reuse detection (was: ADR-005 claimed it, code did not do it).**
  `RefreshTokenService.validateAndRotate` now, on replay of an already-revoked token, revokes the
  user's **entire active token family** and returns 401. The revocation runs in a new
  `RefreshTokenReuseHandler` with `@Transactional(REQUIRES_NEW)` so it commits despite the request's
  401 rollback (separate bean, so the Spring proxy applies). New repo method
  `revokeAllActiveByUserId`.
- **Optimistic locking on `RefreshToken`** (`@Version` + `V3__add_version_to_refresh_tokens.sql`):
  two concurrent rotations of the same token can no longer both succeed; the loser gets an
  optimistic-lock failure, mapped to 401 in `GlobalExceptionHandler`.
- **Prod access-token TTL 1h → 15min** (`application-prod.properties`), per ADR-005.
- **Filter principal now = `AuthenticatedUser(userId, email)`** (authenticates on JWT subject per
  ADR-001, was email). Prep for `/auth/me` + RBAC; authorities still empty (RBAC plugs in here).

### Tests added (+22)
- `JwtAuthenticationFilterTest` (7): valid / expired / forged-signature / malformed / missing /
  non-Bearer / chain-always-continues. (Closed the pre-existing filter coverage gap.)
- `RefreshTokenServiceTest` (8): reuse-detection, rotation, expiry, unknown, logout idempotency.
- `RefreshTokenOptimisticLockTest` (2, `@DataJpaTest`): proves `@Version` actually fires — verified
  by a mutation check (removing `@Version` turns these red).
- `GlobalExceptionHandlerTest` (2): optimistic-lock→401 mapping, generic 500 hides internals.
- Integration (3): multi-device reuse-family revocation (3 devices), case-insensitive-email 409,
  logout idempotency.

### Docs aligned (rule 7)
- `ADR-005`: reuse detection + concurrency guard + prod TTL marked **implemented**.
- `PRD.md`: refresh-token Non-Goal corrected (now delivered).

### Verified by an adversarial 3-lens review workflow
Two CONFIRMED test-coverage findings (optimistic-lock untested; reuse test only size-1 family) were
both fixed here. The correctness-transactions lens hit a session rate limit and did not complete —
its concerns (REQUIRES_NEW commit semantics; bulk-update vs `@Version`) are instead covered
empirically by the passing reuse-family E2E and the optimistic-lock `@DataJpaTest`.

### Still open / next
- **Rate limiting** is still on the unmerged `claude/rate-limit-login` branch — login/signup have no
  brute-force protection on THIS branch. Merge it next; re-add the 429 `@ApiResponse`.
- **V2/V3 Flyway migrations are still not exercised by the suite** (tests use H2 + `ddl-auto`,
  Flyway disabled). A Testcontainers-Postgres test would cover them + the prod-only `LOWER(email)`
  index. Needs Docker (unavailable this session).
- Access-token denylist (`jti`) intentionally NOT built — 15-min TTL judged sufficient for now.

---

## Auth Portal — Milestone 1 PLANNED this session (2026-07-08, not yet built)

A thin BFF web client for this service was scoped and planned. **No backend code was written.**

- **Full plan:** `docs/PORTAL_MILESTONE_1.md` (A–G, implementation-ready, first 10 coding tasks).
- **Scope:** signup, login, logout, protected shell, minimal account page (email+userId), 2 marked
  placeholders. **Out:** password reset, email verification, sessions, RBAC, `/auth/me` (optional).
- **Key decisions:** Next.js App Router + TS; **thin BFF**; `iron-session` **encrypted httpOnly cookie**
  holds the token pair server-side; **refresh token never reaches browser JS / localStorage**; browser
  is same-origin to the BFF so backend CORS is irrelevant; account identity from decoding the
  server-held access token (no `/auth/me` needed for M1).
- **Backend gaps it must render as "unavailable" (do NOT build here):** password reset, email
  verification (`isVerified` unused), sessions/devices API.
- **Optional backend add (only if wanted):** `GET /auth/me` → `{userId,email}` from `AuthenticatedUser`;
  M1 works without it.
- **Exact next step:** create the `web/` project and execute PORTAL_MILESTONE_1.md tasks 1–5
  (Next.js scaffold → BFF login/signup/logout routes → session lib), then 6–10.

---

## Session tooling + uncommitted working tree (2026-07-08)

- **New session skills** added: `/handoff` (`.claude/skills/handoff/SKILL.md` — writes this file) and
  `/resume` (`.claude/skills/resume/SKILL.md` — reads this file + verifies against git/tests to orient
  a continuing session). `CLAUDE.md` skills table updated with both rows.
- **Uncommitted files this session** (grouped; nothing committed yet):
  - *Source (new):* `security/AuthenticatedUser.java`, `service/RefreshTokenReuseHandler.java`
  - *Source (mod):* `exception/GlobalExceptionHandler.java`, `model/RefreshToken.java`,
    `repository/RefreshTokenRepository.java`, `security/JwtAuthenticationFilter.java`,
    `service/RefreshTokenService.java`
  - *Migration:* `db/migration/V3__add_version_to_refresh_tokens.sql`
  - *Config:* `application-prod.properties` (15-min TTL)
  - *Tests (new):* `exception/GlobalExceptionHandlerTest.java`, `repository/RefreshTokenOptimisticLockTest.java`,
    `security/JwtAuthenticationFilterTest.java`, `service/RefreshTokenServiceTest.java`
  - *Tests (mod):* `controller/AuthControllerIntegrationTest.java`
  - *Docs:* `ADR/005`, `PRD.md`, `HANDOFF.md`, `PORTAL_MILESTONE_1.md` (new), `CLAUDE.md`
  - *Tooling:* `.claude/skills/handoff/SKILL.md` (new)

---

> ⚠️ **Everything below is from the 2026-06-19 session and is partly superseded** — e.g. "set 15-min
> prod TTL", the `JwtAuthenticationFilter` test, and `GET /auth/me` labeled "future" are now DONE or
> re-scoped. Trust the sections above and the New Session Prompt at the very bottom for current state.

## Previous Session (2026-06-19 — Open-source readiness + developer experience)

### Security Hardening Round 2 (merged to `main` as PR #9)

These changes landed on `main` independently and are now merged in:

- **`Dockerfile`**: non-root `spring` user in runtime stage (least-privilege container)
- **`SecurityConfig`**: `frameOptions.disable()` → `frameOptions.sameOrigin()` (clickjacking protection)
- **`docker-compose.yml`**: removed demo `JWT_SECRET` fallback that bypassed JwtUtil validation
- **`.env.example`**: demo JWT_SECRET replaced with JwtUtil-rejected placeholder (closes onboarding-path gap)
- **`.github/workflows/ci.yml`**: `upload-artifact@v3` → `@v4` (action deprecation fix)
- **`CLAUDE_SESSION_START.md`**: corrected `APP_JWT_SECRET` → `JWT_SECRET`
- **`docs/RUNBOOK.md`**, **`docs/ARCHITECTURE.md`**: updated to match changes
- **`AuthControllerIntegrationTest`**: `responses_includeXFrameOptionsSameOrigin()` test added

### OpenAPI / Swagger UI

- **`pom.xml`**: Added `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0`
- **`config/OpenApiConfig.java`** (new): `@OpenAPIDefinition` (title, description, version) + `@SecurityScheme(type=HTTP, scheme="bearer", bearerFormat="JWT")` — enables Authorize button in Swagger UI
- **`config/SecurityConfig.java`**: Added `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` to `permitAll()` — without this Swagger 401s
- **`application-prod.properties`**: Added `springdoc.api-docs.enabled=false` + `springdoc.swagger-ui.enabled=false` — Swagger off in prod
- **`controller/AuthController.java`**: Added `@Tag`, `@Operation`, `@ApiResponses` to all 4 endpoints (signup, login, refresh, logout)
- **DTOs**: Added `@Schema` (description, example) to `SignupRequest`, `LoginRequest`, `AuthResponse`, `RefreshRequest`, `LogoutRequest`
- **`exception/ErrorResponse.java`**: Added `@Schema` to record fields

Swagger UI: `http://localhost:8080/swagger-ui.html`
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Contributor Readiness

- **`CONTRIBUTING.md`** (new): branch naming, coding rules, test expectations, ADR process, security contact email
- **`CODE_OF_CONDUCT.md`** (new): Contributor Covenant v2.1
- **`.github/ISSUE_TEMPLATE/bug_report.md`** (new): structured bug report
- **`.github/ISSUE_TEMPLATE/feature_request.md`** (new): ADR checkbox, acceptance criteria checklist
- **`.github/PULL_REQUEST_TEMPLATE.md`** (new): docs-update checklist, security checklist, breaking-change declaration

### Deployment Docs

- **`docs/DEPLOYMENT.md`** (new): environment variables table, profile comparison, Docker deployment steps, JAR deployment, cloud platform notes (Railway, ECS, Cloud Run), production checklist, rollback guidance, "what's not prod-ready" table

### Observability

- **`docs/OBSERVABILITY.md`** (new): logging baseline (recommended events, not yet implemented), dev vs prod log levels, structured JSON logging setup guide, monitoring signals table with alert thresholds, Actuator health endpoint usage, future audit log event table

### MCP Direction

- **`docs/ADR/006-mcp-agent-auth-architecture.md`** extended: added "Where to Start" section for MCP contributors — separate-project pattern, suggested MCP tool names mapping to REST endpoints, relationship to README

### README

- Updated CI badge (was "not configured", now "GitHub Actions")
- Added `JWT_SECRET` export step to Quick Start
- Added Swagger UI step (step 4) to Quick Start
- Added interactive docs pointer to API Reference section
- Updated file structure tree to match actual repo
- Updated test inventory counts
- Updated coverage gaps (Good First Issues framing)
- Updated "What's Done" section: full current feature list (Phase 1 + 2 + 3)
- Updated Roadmap: completed items marked ✅, remaining items as priority table with GitHub labels

---

## Outstanding Risks or Blockers

- ⚠️ **V2/V3 Flyway migrations untestable locally** — tests use H2 + `ddl-auto` (Flyway disabled); PostgreSQL Testcontainers would cover the migrations and the prod-only `LOWER(email)` index
- ⚠️ **Rate-limit branch unmerged** — `claude/rate-limit-login` is separate and should be merged first (or in parallel); 429 `@ApiResponse` intentionally removed from `AuthController` on this branch since rate limiting is not present here — will be re-added when `claude/rate-limit-login` merges
- ✅ **Concurrent refresh race condition** — RESOLVED this session: `@Version` optimistic lock on `RefreshToken` (V3 migration), loser mapped to 401
- ✅ **Access token TTL** — RESOLVED this session: prod set to 15 min (`application-prod.properties`)
- ℹ️ **Access-token revocation** — a logged-out/rotated access JWT is still valid until expiry (now ≤15 min in prod). No `jti` denylist built; judged acceptable at current scale

---

## Files Changed (This Session)

### New source files
| File | Purpose |
|------|---------|
| `src/main/java/com/authplatform/config/OpenApiConfig.java` | OpenAPI definition + security scheme |

### Modified source files
| File | Change |
|------|--------|
| `pom.xml` | Added `springdoc-openapi-starter-webmvc-ui:2.5.0` |
| `src/main/java/com/authplatform/config/SecurityConfig.java` | Swagger URLs + CORS bean added; `sameOrigin()` kept from security-hardening PR |
| `src/main/java/com/authplatform/controller/AuthController.java` | OpenAPI annotations on all endpoints |
| `src/main/java/com/authplatform/dto/SignupRequest.java` | `@Schema` annotations |
| `src/main/java/com/authplatform/dto/LoginRequest.java` | `@Schema` annotations |
| `src/main/java/com/authplatform/dto/AuthResponse.java` | `@Schema` annotations |
| `src/main/java/com/authplatform/dto/RefreshRequest.java` | `@Schema` annotations |
| `src/main/java/com/authplatform/dto/LogoutRequest.java` | `@Schema` annotations |
| `src/main/java/com/authplatform/exception/ErrorResponse.java` | `@Schema` annotations |
| `src/main/resources/application-prod.properties` | Swagger disabled in prod |
| `src/test/java/com/authplatform/controller/AuthControllerIntegrationTest.java` | +3 tests: X-Frame-Options, OpenAPI docs, Swagger UI |

### New doc/repo files
| File | Purpose |
|------|---------|
| `CONTRIBUTING.md` | Full contributor guide |
| `CODE_OF_CONDUCT.md` | Contributor Covenant v2.1 |
| `.github/ISSUE_TEMPLATE/bug_report.md` | Bug issue template |
| `.github/ISSUE_TEMPLATE/feature_request.md` | Feature request template |
| `.github/PULL_REQUEST_TEMPLATE.md` | PR template |
| `docs/DEPLOYMENT.md` | Deployment guide |
| `docs/OBSERVABILITY.md` | Logging + monitoring baseline |

### Docs updated
- `docs/ADR/006-mcp-agent-auth-architecture.md` — extended with contributor guide
- `docs/PROJECT_PROGRESS.md` — Milestones updated
- `docs/PROJECT_BACKLOG.md` — open-source readiness sprint marked complete
- `README.md` — badges, quick-start, file structure, test inventory, roadmap

---

## Tests Run & Results

```
Tests run: 27, Failures: 0, Errors: 0 — AuthControllerIntegrationTest
Tests run:  8, Failures: 0, Errors: 0 — JwtUtilTest
Tests run:  5, Failures: 0, Errors: 0 — AuthServiceTest
─────────────────────────────────────────────────────
Tests run: 40, Failures: 0, Errors: 0 — BUILD SUCCESS
```

New tests (this merge): `responses_includeXFrameOptionsSameOrigin`, `openApiDocs_returns200_withoutAuth`, `swaggerUi_isReachable`

---

## Exact Next Steps

### Priority 1: Open PRs and merge
1. Merge `claude/rate-limit-login` → `main` (simpler, no conflicts with this branch)
2. Merge `claude/refresh-tokens` → `main` (this branch — includes all three sprints + security hardening merge)
3. Update `main` branch badges after merge (test count stays 40; CI badge auto-updates)

### Priority 2: Good First Issues to label in GitHub
These are well-scoped, documented, and low-risk for external contributors:
- `GET /auth/me` — return user info (ID, email) from JWT claims; low effort
- Merge `claude/rate-limit-login` — standalone, zero conflict
- HikariCP connection pool config in `application-prod.properties`
- PostgreSQL Testcontainers integration test — validates V2 migration
- `JwtAuthenticationFilter` unit test — coverage gap, no Spring context needed

### Priority 3: Production hardening
- Set `app.jwt.expiration-ms=900000` (15 min) in prod env
- Configure `app.cors.allowed-origins` to actual frontend domain before going live
- TLS at load balancer (nginx/Caddy/ALB)

### Standard process
1. Read `CLAUDE.md` + this `HANDOFF.md` first
2. Run `mvn test` (must stay at 40 passing, 0 failures)
3. For any auth/config change: run `/security-review`
4. Update `HANDOFF.md` before ending session

---

## Summary

Sprint added: OpenAPI/Swagger UI (working, disabled in prod), contributor docs (CONTRIBUTING, CODE_OF_CONDUCT, GitHub templates), DEPLOYMENT.md (deployment guide), OBSERVABILITY.md (logging/monitoring baseline), and ADR-006 extended with MCP contributor guide. Security hardening PR #9 merged in (sameOrigin frame options, non-root Dockerfile, .env.example fix). README fully updated. Security review passed (2 warnings, 0 failures). 40 tests passing. Branch `claude/refresh-tokens` is ready for PR.

---

## 🔖 New Session Prompt (ready to paste)

> Continue the Java Auth Service project (branch `claude/refresh-tokens`). This is the authoritative
> starting state — do not re-derive it.
>
> **Read first, in order:** `CLAUDE.md`, this `docs/HANDOFF.md` (top sections only — the pre-2026-06-19
> tail is superseded), `docs/PORTAL_MILESTONE_1.md`, `docs/ADR/005-refresh-token-design.md`.
>
> **Current backend state (done, do NOT redo):** signup/login/refresh/logout + `/actuator/health`;
> HS256 JWT (`sub`=userId, `email` claim), 15-min prod access TTL, 7-day refresh with rotation +
> **reuse detection** (family revoke via `RefreshTokenReuseHandler` REQUIRES_NEW) + **`@Version`
> optimistic lock** (V3 migration); principal is `AuthenticatedUser(userId,email)`. **62 tests pass.**
> Build with IntelliJ's bundled Maven + JBR 21 (NOT system JDK 24): set `JAVA_HOME` to the JBR path,
> run the bundled `mvn -B test`.
>
> **Backend gaps (do NOT invent these):** no `/auth/me`, no password reset, no email verification
> (`isVerified` unused), no sessions/devices API, no RBAC. Rate limiting lives on the separate
> unmerged `claude/rate-limit-login` branch.
>
> **What to do next:** implement **Auth Portal Milestone 1** exactly per `docs/PORTAL_MILESTONE_1.md`
> — start a new `web/` Next.js (App Router + TS) project and execute tasks 1–5, then 6–10. Constraints:
> thin BFF, `iron-session` encrypted httpOnly cookie, **refresh token never in browser JS/localStorage**,
> scope limited to signup/login/logout/protected-shell/account + 2 marked-unavailable placeholders.
> Ignore AI Guardline entirely. Do not widen backend scope.
>
> **Note:** all of this session's work (backend hardening + docs + the `/handoff` skill) is
> **uncommitted** on `claude/refresh-tokens` — decide with the user whether to commit before building.
> A `/handoff` skill exists (`.claude/skills/handoff/SKILL.md`); run `/handoff` before stopping.
