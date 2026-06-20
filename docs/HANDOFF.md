# Session Handoff

**Last updated**: 2026-06-19 (Open-source readiness + developer experience sprint)
**Branch**: `claude/refresh-tokens` (off `main`)
**Tests**: 40 passing (0 failures) — `mvn test` BUILD SUCCESS

---

## What Was Completed This Session

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

- ⚠️ **V2 Flyway migration untestable locally** — same as previous session; PostgreSQL Testcontainers would give full coverage
- ⚠️ **Rate-limit branch unmerged** — `claude/rate-limit-login` is separate and should be merged first (or in parallel); 429 `@ApiResponse` intentionally removed from `AuthController` on this branch since rate limiting is not present here — will be re-added when `claude/rate-limit-login` merges
- ⚠️ **Concurrent refresh race condition** — documented in ADR-005; acceptable for current scale
- ⚠️ **Access token TTL still 1 hour** — should be 15 min in production; pure env config change

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
