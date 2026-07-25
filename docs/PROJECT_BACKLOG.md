# Project Backlog

Items are ordered by priority within each section. Phase 1 is complete; all items below are Phase 2+.

---

## Critical

_(Nothing blocking. Phase 1 is complete and stable.)_

---

## High Priority

### ✅ PostgreSQL Migration — COMPLETE (commit e51d94f)
- PostgreSQL driver + Flyway in `pom.xml` ✓
- `application-prod.properties` with env-driven DB config ✓
- `V1__create_users_table.sql` Flyway migration ✓
- H2 kept for local dev; prod uses PostgreSQL ✓
- ⚠ Remaining: `LOWER(email)` functional unique index not yet added

### ✅ Docker / Docker Compose — COMPLETE (commit bb6df9e + 0eeeef4)
- Multi-stage `Dockerfile` (Maven build → JRE Alpine, non-root user) ✓
- `docker-compose.yml` with `postgres` + `app` services, healthchecks ✓
- `.env.example` documenting `JWT_SECRET`, `POSTGRES_*` (placeholder, not demo secret) ✓
- `/actuator/health` endpoint ✓

### ✅ CI/CD Pipeline — COMPLETE (commit fd08df2 + 0eeeef4)
- `.github/workflows/ci.yml`: JDK 17, `mvn -B verify`, JaCoCo report upload ✓
- Triggers on push to `main` and `claude/**`, PR to `main` ✓
- `upload-artifact@v4` (upgraded from deprecated v3) ✓

---

## Recently Completed

### ✅ Open-Source Readiness Sprint (branch: claude/refresh-tokens, 2026-06-19)
- **OpenAPI / Swagger UI**: `springdoc-openapi-starter-webmvc-ui:2.5.0`; `/swagger-ui.html`; `/v3/api-docs`; disabled in prod
- **`OpenApiConfig.java`**: `@OpenAPIDefinition` + `@SecurityScheme` (bearerAuth JWT)
- **All endpoints** annotated: `@Tag`, `@Operation`, `@ApiResponse`, `@Schema` on DTOs
- **`CONTRIBUTING.md`**: branch guide, coding rules, test expectations, ADR process
- **`CODE_OF_CONDUCT.md`**: Contributor Covenant v2.1
- **GitHub issue templates** (bug, feature) + PR template (security checklist)
- **`docs/DEPLOYMENT.md`**: profiles, env vars, Docker, JAR, cloud, production checklist, rollback
- **`docs/OBSERVABILITY.md`**: logging baseline, monitoring signals, future JSON logging guide
- **`docs/ADR/006`** extended with MCP contributor guide
- **`README.md`**: updated throughout — badges, Swagger quick-start, accurate roadmap

### ✅ Refresh Tokens (branch: claude/refresh-tokens, 2026-06-19)
- `POST /auth/refresh` — validates refresh token, returns new token pair (rotated)
- `POST /auth/logout` — revokes refresh token; idempotent
- Signup and login both return `refreshToken` in response
- Stored as SHA-256 hash in DB; 7-day TTL; token rotation on every refresh
- V2 Flyway migration (`refresh_tokens` table)
- 10 new integration tests

### ✅ CORS (branch: claude/refresh-tokens, 2026-06-19)
- `CorsConfigurationSource` bean in `SecurityConfig`
- Configurable via `app.cors.allowed-origins` (default: localhost:3000, localhost:5173)
- Tested with integration test for `Access-Control-Allow-Origin` header

### ✅ Rate Limiting (branch: claude/rate-limit-login, 2026-06-19)
- `POST /auth/login` — 10 attempts per 10 minutes per IP, 429 + `Retry-After` header
- Bucket4j in-process, `ConcurrentHashMap<IP, Bucket>`, no Redis dependency
- ADR-005 on rate-limit branch (to be renumbered on merge)

---

## Medium Priority

### Rate Limiting — `/auth/signup`
- Currently only `/auth/login` is rate-limited; signup should also be protected
- Same Bucket4j pattern, separate interceptor or shared config

### ✅ `/auth/me` Endpoint — COMPLETE (merged from `claude/auth-me`)
- `GET /auth/me` — returns current user's id, email, verified flag, createdAt
- Reads `userId` from the `AuthenticatedUser` principal (JWT subject), not email — consistent with ADR-001

### ✅ Rate Limiting on /auth/login — COMPLETE (merged from `claude/rate-limit-login`)
- Bucket4j in-process token bucket; 10 attempts / 10 min / IP
- 429 Too Many Requests + `Retry-After` header on breach
- Keyed by `remoteAddr` (not XFF — attacker-controlled)
- ADR 005 documents design choices
- ⚠ Remaining: `/auth/signup` not yet rate-limited

### Audit Logging
- Structured log on every auth event: signup, login success, login failure, token rejection
- Include: timestamp, email (masked), IP, user agent, outcome
- Future: persist to audit table for compliance and agent traceability

### Token Revocation / Denylist
- On logout or password change, add JWT `jti` (JWT ID) to a short-TTL denylist (Redis or DB)
- `JwtAuthenticationFilter` checks denylist before accepting token
- Required for security-sensitive deployments

### ✅ `/auth/me` Endpoint — COMPLETE (branch `claude/auth-me`)
- `GET /auth/me` — returns id, email, verified, createdAt from database (DB-backed for richer response)
- Requires valid Bearer token; 401 on missing/invalid token or deleted user

---

## Future Ideas / Agent-Native Roadmap

### MCP (Model Context Protocol) Server
- Expose authentication operations as MCP tools so AI agents can authenticate API calls autonomously
- Tools: `signup`, `login`, `validate_token`, `refresh_token`
- Session-aware: agent holds a token in its context and re-authenticates when it expires
- This is the core "Agent-Native Authentication Platform" vision

### Machine-Readable Permission Scopes
- Replace coarse `authenticated` / `unauthenticated` with capability-based scopes in JWT claims
- Example claims: `"scopes": ["data:read", "data:write", "admin:users"]`
- Agents request minimal scopes; the platform enforces least-privilege

### API Keys (Agent Credentials)
- Long-lived API keys for non-interactive agents (no browser flow needed)
- `POST /auth/api-keys` — create key; `DELETE /auth/api-keys/{id}` — revoke
- Keys stored hashed; presented as `Authorization: ApiKey <key>`
- Rate-limited and audited separately from JWT flows

### Email Verification / OTP
- Send verification email on signup; block login until verified
- OTP flow for password reset
- Requires SMTP integration (SendGrid, Resend, SES)

### Role-Based Access Control (RBAC)
- `roles` claim in JWT (e.g., `["USER", "ADMIN"]`)
- Spring Security `@PreAuthorize` or method security
- Admin endpoints for user management

### External Auth Providers
- OAuth2 / OIDC login (Google, GitHub)
- SAML for enterprise SSO
- Map external identity to internal `User` record

### Multi-Tenancy
- Tenant ID in JWT claims and database schema
- Row-level security in PostgreSQL
- Per-tenant JWT secret rotation

### Session Management Dashboard
- List active tokens/sessions for a user
- Revoke individual sessions
- Show device/IP/last-used metadata (requires audit log)
