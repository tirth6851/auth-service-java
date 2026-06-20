# Project Backlog

Items are ordered by priority within each section. Phase 1 is complete; all items below are Phase 2+.

---

## Critical

_(Nothing blocking. Phase 1 is complete and stable.)_

---

## High Priority

### PostgreSQL Migration
Replace H2 in-memory with PostgreSQL.
- Add `spring-boot-starter-data-jpa` PostgreSQL driver dependency
- Replace H2 datasource config with Postgres connection string (env-var driven)
- Add Flyway or Liquibase for schema migrations (replace `ddl-auto=update`)
- Add `LOWER(email)` functional unique index for case-insensitive uniqueness
- Update `docker-compose.yml` (create it) with Postgres service
- Update `src/test/resources/application.properties` to use H2 or TestContainers

### Docker / Docker Compose
- `Dockerfile` (multi-stage build: Maven compile → JRE runtime image)
- `docker-compose.yml` with `app` + `postgres` services
- `.env.example` documenting required env vars (`JWT_SECRET`, `POSTGRES_*`)
- Health check endpoint (`/actuator/health` or custom `/health`)

### CI/CD Pipeline
- GitHub Actions workflow: compile → test → build image → push to registry
- Enforce `mvn test` on every PR targeting `main`
- Secret scanning step (detect committed `JWT_SECRET`)
- Optional: deploy to a cloud target (Railway, Fly.io, Render) on merge to `main`

---

## Recently Completed

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

### `/auth/me` Endpoint
- `GET /auth/me` — return current user's ID and email from the JWT claims
- Useful for clients to verify token validity and retrieve identity without a separate DB call

### Audit Logging
- Structured log on every auth event: signup, login success, login failure, token rejection
- Include: timestamp, email (masked), IP, user agent, outcome
- Future: persist to audit table for compliance and agent traceability

### Token Revocation / Denylist
- On logout or password change, add JWT `jti` (JWT ID) to a short-TTL denylist (Redis or DB)
- `JwtAuthenticationFilter` checks denylist before accepting token
- Required for security-sensitive deployments

### `/auth/me` Endpoint
- `GET /auth/me` — return current user's ID and email from the JWT claims
- Useful for clients to verify token validity and retrieve identity without a separate DB call

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
