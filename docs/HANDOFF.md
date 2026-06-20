# Session Handoff

**Last updated**: 2026-06-19 (Refresh tokens + logout + CORS + actuator security sprint)  
**Branch**: `claude/refresh-tokens` (off `main`)  
**Tests**: 37 passing (0 failures) — `mvn test` BUILD SUCCESS  

---

## What Was Completed This Session

### Main Feature — Refresh Tokens + Logout

- **`RefreshToken` entity** (`model/RefreshToken.java`): `id`, `token` (SHA-256 hash), `userId`, `expiresAt`, `createdAt`, `revokedAt`
- **`RefreshTokenRepository`** (`repository/RefreshTokenRepository.java`): `findByToken(String)`
- **`RefreshTokenService`** (`service/RefreshTokenService.java`):
  - `createToken(Long userId)` — generates UUID, stores SHA-256 hash, returns raw UUID to caller
  - `validateAndRotate(String rawToken)` — validates (not expired, not revoked), sets `revokedAt`, returns entity; `@Transactional`
  - `revokeToken(String rawToken)` — for logout; returns 401 if not found, 204 if already revoked (idempotent)
  - SHA-256 hashing via Java 17 `MessageDigest` + `HexFormat` — no extra dependencies
- **`AuthService`** updated: `signup` and `login` now return a token pair; `refresh` and `logout` methods added; `@Transactional` on `refresh`
- **`AuthController`** updated: `POST /auth/refresh` (returns 200 + new pair) and `POST /auth/logout` (returns 204)
- **`RefreshRequest.java`**, **`LogoutRequest.java`** DTOs
- **`AuthResponse.java`** updated: added `refreshToken` field; `@JsonInclude(NON_NULL)` added — backward-compatible
- **`V2__create_refresh_tokens_table.sql`** Flyway migration for PostgreSQL prod (dev/test use H2 + `ddl-auto=update`)

### CORS Configuration

- `CorsConfigurationSource` bean in `SecurityConfig.java`
- Allowed origins configured via `app.cors.allowed-origins` (`@Value` with default `http://localhost:3000,http://localhost:5173`)
- `.cors(cors -> cors.configurationSource(...))` registered before CSRF in the security chain
- Allows: `GET, POST, PUT, DELETE, OPTIONS` + `Authorization, Content-Type` headers + credentials

### Actuator Health Security

- `/actuator/health` added to `permitAll()` in `SecurityConfig` — accessible without JWT
- Was already exposed via `management.endpoints.web.exposure.include=health`

### Tests (10 new integration tests)

- `signup_returnsRefreshToken` — refreshToken present in signup response
- `login_returnsRefreshToken` — refreshToken present in login response
- `refresh_returnsNewTokenPair_whenValid` — happy path refresh
- `refresh_returns401_whenExpired` — seeds expired row via `refreshTokenRepository` directly (no Thread.sleep)
- `refresh_returns401_whenRevoked` — logout then refresh → 401
- `refresh_returns401_whenInvalidToken` — garbage token → 401
- `logout_returns204_whenValid` — happy path logout
- `logout_returns401_whenInvalidToken` — garbage token → 401
- `actuatorHealth_returns200_withoutAuth` — health without JWT → 200
- `cors_allowedOriginReceivesCorsHeaders` — `Access-Control-Allow-Origin` header present

`AuthServiceTest` updated: added `@Mock RefreshTokenService refreshTokenService` + updated constructor call (3 args → 4).

### ADRs

- **ADR-005** (`docs/ADR/005-refresh-token-design.md`): refresh token rationale (SHA-256 vs bcrypt, rotation, revocation model, known limitations)
- **ADR-006** (`docs/ADR/006-mcp-agent-auth-architecture.md`): MCP/agent-auth dual-mode architecture note

### Docs Updated

- `docs/API_CONTRACT.md` — complete rewrite: correct response shapes, all 5 endpoints, CORS section, error format
- `docs/PROJECT_PROGRESS.md` — Milestone 9 added
- `docs/PROJECT_BACKLOG.md` — refresh tokens + CORS marked complete; backlog reorganized
- `CLAUDE_SESSION_START.md` — fixed `APP_JWT_SECRET` typo → `JWT_SECRET`; updated API list

---

## Outstanding Risks or Blockers

- ⚠️ **V2 Flyway migration untestable locally** — Flyway is disabled in dev/test (H2 with `ddl-auto=update`). The migration `V2__create_refresh_tokens_table.sql` runs only against PostgreSQL in production. It was hand-validated against the `RefreshToken` entity's JPA mapping (snake_case columns, nullable `revoked_at`, `BIGINT REFERENCES users(id)`). CI with PostgreSQL testcontainers would give full coverage.
- ⚠️ **Concurrent refresh race condition** — Two simultaneous refreshes with the same token can both succeed under `READ_COMMITTED` isolation. Documented in ADR-005. Mitigatable with `@Version` optimistic locking if needed.
- ⚠️ **Access token TTL still 1 hour** — With refresh tokens deployed, production should reduce to 15 min (`app.jwt.expiration-ms=900000`). Noted in ADR-005. No code change needed; just env config.
- ⚠️ **Rate-limit branch unmerged** — `claude/rate-limit-login` has rate limiting for `POST /auth/login` (Bucket4j). It's not yet on `main`. The two branches diverged from main independently, so they should be merged separately (rate-limit first or in parallel).

---

## Files Changed (This Session)

### New source files
| File | Purpose |
|------|---------|
| `src/main/java/com/authplatform/model/RefreshToken.java` | JPA entity |
| `src/main/java/com/authplatform/repository/RefreshTokenRepository.java` | Spring Data JPA |
| `src/main/java/com/authplatform/service/RefreshTokenService.java` | Issue/validate/rotate/revoke |
| `src/main/java/com/authplatform/dto/RefreshRequest.java` | POST /auth/refresh request DTO |
| `src/main/java/com/authplatform/dto/LogoutRequest.java` | POST /auth/logout request DTO |
| `src/main/resources/db/migration/V2__create_refresh_tokens_table.sql` | PostgreSQL migration |
| `docs/ADR/005-refresh-token-design.md` | ADR |
| `docs/ADR/006-mcp-agent-auth-architecture.md` | ADR |

### Modified source files
| File | Change |
|------|--------|
| `src/main/java/com/authplatform/dto/AuthResponse.java` | Added `refreshToken` field + `@JsonInclude(NON_NULL)` |
| `src/main/java/com/authplatform/service/AuthService.java` | Integrated RefreshTokenService; added refresh/logout methods |
| `src/main/java/com/authplatform/controller/AuthController.java` | Added POST /auth/refresh + POST /auth/logout |
| `src/main/java/com/authplatform/config/SecurityConfig.java` | Added CORS bean + `/actuator/health` to permitAll |
| `src/main/resources/application.properties` | Added CORS + refresh TTL + Jackson timestamp config |
| `src/test/resources/application.properties` | Added CORS + refresh TTL + Jackson timestamp config |
| `src/test/java/com/authplatform/controller/AuthControllerIntegrationTest.java` | 10 new tests + `@Autowired RefreshTokenRepository` |
| `src/test/java/com/authplatform/service/AuthServiceTest.java` | Added `@Mock RefreshTokenService` |

### Docs updated
- `docs/API_CONTRACT.md`, `docs/PROJECT_PROGRESS.md`, `docs/PROJECT_BACKLOG.md`, `docs/HANDOFF.md`, `CLAUDE_SESSION_START.md`

---

## Tests Run & Results

```
Tests run: 24, Failures: 0, Errors: 0 — AuthControllerIntegrationTest
Tests run:  8, Failures: 0, Errors: 0 — JwtUtilTest
Tests run:  5, Failures: 0, Errors: 0 — AuthServiceTest
─────────────────────────────────────────────────────
Tests run: 37, Failures: 0, Errors: 0 — BUILD SUCCESS
```

---

## Exact Next Steps

### Priority 1: Merge strategy (choose one)
- Open PR for `claude/refresh-tokens` → merge to main
- Optionally merge `claude/rate-limit-login` first (simpler, no conflict with this branch)
- Then open PR for this branch on top of updated main

### Priority 2: Production access token TTL
- Set `app.jwt.expiration-ms=900000` (15 min) in production environment
- No code change needed; pure config

### Priority 3: Next feature candidates
- `GET /auth/me` — return user info from JWT claims (low effort, high client value)
- CI pipeline with PostgreSQL testcontainers — validates V2 migration
- Rate limiting for `/auth/signup` — extend the Bucket4j interceptor
- RBAC / scope claims — add `scopes` to JWT payload

### Standard process
1. Read `CLAUDE.md` + this `HANDOFF.md` first
2. Run `mvn test` (must stay at 37 passing, 0 failures)
3. For any auth/config change: run `/security-review`
4. Update `HANDOFF.md` before ending session

---

## Summary

Implemented refresh tokens + logout (main feature), CORS, and `/actuator/health` security fix in a single sprint. 37 tests pass. V2 Flyway migration ready for PostgreSQL. Two ADRs added. Branch `claude/refresh-tokens` is ready for PR.
