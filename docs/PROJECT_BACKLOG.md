# Project Backlog

Items are ordered by priority within each section. Phase 1 is complete; all items below are Phase 2+.

---

## Critical

_(Nothing blocking. Phase 1 is complete and stable.)_

---

## High Priority

_(Nothing currently queued — see Medium Priority below.)_

---

## Recently Completed

### ✅ PostgreSQL Migration (commit e51d94f)
- PostgreSQL driver + Flyway in `pom.xml`
- `application-prod.properties` with env-driven DB config
- `V1__create_users_table.sql` Flyway migration
- H2 kept for local dev; prod uses PostgreSQL
- ⚠ Remaining: `LOWER(email)` functional unique index not yet added

### ✅ Docker / Docker Compose (commit bb6df9e + 0eeeef4)
- Multi-stage `Dockerfile` (Maven build → JRE Alpine, non-root user)
- `docker-compose.yml` with `postgres` + `app` services, healthchecks
- `.env.example` documenting `JWT_SECRET`, `POSTGRES_*` (placeholder, not demo secret)
- `/actuator/health` endpoint

### ✅ CI/CD Pipeline (commit fd08df2 + 0eeeef4)
- `.github/workflows/ci.yml`: JDK 17, `mvn -B verify`, JaCoCo report upload
- Triggers on push to `main` and `claude/**`, PR to `main`
- `upload-artifact@v4`

### ✅ Refresh Tokens, Logout, CORS, Actuator Security (PR #10)
- `POST /auth/refresh` — validates refresh token, returns new token pair (rotated)
- `POST /auth/logout` — revokes refresh token; idempotent
- Signup and login both return `refreshToken` in response
- Stored as SHA-256 hash in DB; 7-day TTL; token rotation on every refresh (ADR-005)
- V2 Flyway migration (`refresh_tokens` table)
- `CorsConfigurationSource` bean, configurable via `app.cors.allowed-origins`
- `/actuator/health` added to `permitAll()`

### ✅ Open-Source Readiness Sprint (PR #10)
- OpenAPI / Swagger UI (`springdoc-openapi`), disabled in prod
- `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, GitHub issue/PR templates
- `docs/DEPLOYMENT.md`, `docs/OBSERVABILITY.md`
- `docs/ADR/006-mcp-agent-auth-architecture.md`

### ✅ GET /auth/me Endpoint (branch `claude/auth-me`)
- Returns `id`, `email`, `verified`, `createdAt` from the database (DB-backed for a richer response than JWT claims alone)
- Requires a valid Bearer token; explicitly excluded from `SecurityConfig` `permitAll` — 401 on missing/invalid token or deleted user

### ✅ Rate Limiting on /auth/login (branch `claude/rate-limit-login`)
- Bucket4j in-process token bucket; 10 attempts / 10 min / IP
- 429 Too Many Requests + `Retry-After` header on breach
- Keyed by `remoteAddr` (not XFF — attacker-controlled)
- ADR-007 documents design choices (renumbered from ADR-005 on the source branch to avoid collision with the refresh-token ADR-005)
- ⚠ Remaining: `/auth/signup` not yet rate-limited

---

## Medium Priority

### Rate Limiting — `/auth/signup`
- Currently only `/auth/login` is rate-limited; signup should also be protected
- Same Bucket4j pattern, separate interceptor or shared config

### Audit Logging
- Structured log on every auth event: signup, login success, login failure, token rejection
- Include: timestamp, email (masked), IP, user agent, outcome
- Future: persist to audit table for compliance and agent traceability

### Token Revocation / Denylist
- On logout or password change, add JWT `jti` (JWT ID) to a short-TTL denylist (Redis or DB)
- `JwtAuthenticationFilter` checks denylist before accepting token
- Required for security-sensitive deployments

### `LOWER(email)` Functional Index
- Add a case-insensitive unique index on `users.email` in PostgreSQL (carried over from the PostgreSQL Migration item)

### Concurrent Refresh Race Condition
- Documented limitation in ADR-005: concurrent refresh with the same token under READ_COMMITTED isolation can both succeed
- Mitigation: `@Version` optimistic locking on `RefreshToken`
- Low priority — risk is negligible at current scale

### Production Access Token TTL
- Access tokens default to 1 hour (`app.jwt.expiration-ms=3600000`)
- With refresh tokens deployed, production should reduce this to 15 minutes (`900000`) — pure env config change

### PostgreSQL Testcontainers
- Replace H2-only test profile with Testcontainers-backed PostgreSQL integration tests
- Validates Flyway migrations (`V1`, `V2`) against the real production engine

---

## Future Ideas / Agent-Native Roadmap

### MCP (Model Context Protocol) Server
- Expose authentication operations as MCP tools so AI agents can authenticate API calls autonomously
- Tools: `signup`, `login`, `validate_token`, `refresh_token`
- Session-aware: agent holds a token in its context and re-authenticates when it expires
- This is the core "Agent-Native Authentication Platform" vision — see ADR-006

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
- `User.verified` field already exists (always `false` today) — `/auth/me` already exposes it
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
