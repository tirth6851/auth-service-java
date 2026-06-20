# Session Handoff

**Last updated**: 2026-06-19 (Open-source readiness + developer experience sprint)  
**Branch**: `claude/refresh-tokens` (off `main`)  
**Tests**: 39 passing (0 failures) — `mvn test` BUILD SUCCESS  

---

## What Was Completed This Session

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

- **`docs/OBSERVABILITY.md`** (new): logging baseline (what to log / not log), dev vs prod log levels, structured JSON logging setup guide (next step via logstash-logback-encoder), monitoring signals table with alert thresholds, Actuator health endpoint usage, future audit log event table

### MCP Direction

- **`docs/ADR/006-mcp-agent-auth-architecture.md`** extended: added "Where to Start" section for MCP contributors — separate-project pattern, suggested MCP tool names mapping to REST endpoints, relationship to README

### README

- Updated CI badge (was "not configured", now "GitHub Actions")
- Added `JWT_SECRET` export step to Quick Start
- Added Swagger UI step (step 4) to Quick Start
- Added interactive docs pointer to API Reference section
- Updated file structure tree to match actual repo
- Updated test inventory counts (accurate: 8 JwtUtil, 5 AuthService, 24 integration)
- Updated coverage gaps (Good First Issues framing)
- Updated "What's Done" section: full current feature list (Phase 1 + 2 + 3)
- Updated Roadmap: completed items marked ✅, remaining items as priority table with GitHub labels

---

## Outstanding Risks or Blockers

- ⚠️ **V2 Flyway migration untestable locally** — same as previous session; PostgreSQL Testcontainers would give full coverage
- ⚠️ **Rate-limit branch unmerged** — `claude/rate-limit-login` is separate and should be merged first (or in parallel); 429 `@ApiResponse` was intentionally removed from `AuthController` on this branch since rate limiting is not present here — will be re-added when `claude/rate-limit-login` merges
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
| `src/main/java/com/authplatform/config/SecurityConfig.java` | Swagger URLs added to `permitAll()` |
| `src/main/java/com/authplatform/controller/AuthController.java` | OpenAPI annotations on all endpoints |
| `src/main/java/com/authplatform/dto/SignupRequest.java` | `@Schema` annotations |
| `src/main/java/com/authplatform/dto/LoginRequest.java` | `@Schema` annotations |
| `src/main/java/com/authplatform/dto/AuthResponse.java` | `@Schema` annotations |
| `src/main/java/com/authplatform/dto/RefreshRequest.java` | `@Schema` annotations |
| `src/main/java/com/authplatform/dto/LogoutRequest.java` | `@Schema` annotations |
| `src/main/java/com/authplatform/exception/ErrorResponse.java` | `@Schema` annotations |
| `src/main/resources/application-prod.properties` | Swagger disabled in prod |

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
- `docs/PROJECT_PROGRESS.md` — Milestone 10 added
- `docs/PROJECT_BACKLOG.md` — open-source readiness sprint marked complete
- `README.md` — badges, quick-start, file structure, test inventory, roadmap

---

## Tests Run & Results

```
Tests run: 26, Failures: 0, Errors: 0 — AuthControllerIntegrationTest
Tests run:  8, Failures: 0, Errors: 0 — JwtUtilTest
Tests run:  5, Failures: 0, Errors: 0 — AuthServiceTest
─────────────────────────────────────────────────────
Tests run: 39, Failures: 0, Errors: 0 — BUILD SUCCESS
```

New tests (this session): `openApiDocs_returns200_withoutAuth`, `swaggerUi_isReachable`

---

## Exact Next Steps

### Priority 1: Open PRs and merge
1. Merge `claude/rate-limit-login` → `main` (simpler, no conflicts with this branch)
2. Merge `claude/refresh-tokens` → `main` (this branch — includes all three sprints)
3. Update `main` branch badges after merge (test count stays 37; CI badge auto-updates)

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
2. Run `mvn test` (must stay at 37 passing, 0 failures)
3. For any auth/config change: run `/security-review`
4. Update `HANDOFF.md` before ending session

---

## Summary

Sprint added: OpenAPI/Swagger UI (working, disabled in prod), contributor docs (CONTRIBUTING, CODE_OF_CONDUCT, GitHub templates), DEPLOYMENT.md (deployment guide), OBSERVABILITY.md (logging/monitoring baseline), and ADR-006 extended with MCP contributor guide. README fully updated. Security review passed (2 warnings, 0 failures). 39 tests passing (2 new Swagger integration tests added). Branch `claude/refresh-tokens` now carries all Phase 2 + 3 work and is ready for PR.
