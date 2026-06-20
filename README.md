# auth-service-java

![Java 17](https://img.shields.io/badge/Java-17-blue?logo=openjdk)
![Spring Boot 3.2.5](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?logo=springboot)
![Tests](https://img.shields.io/badge/tests-37%20passing-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)
![CI](https://img.shields.io/badge/CI-GitHub%20Actions-brightgreen?logo=githubactions)

A stateless JWT authentication REST API built with Spring Boot 3.2 and Java 17. Provides signup, login, refresh token rotation, and logout with BCrypt password hashing and HS256 JWT issuance. Fully functional for local development with Docker + PostgreSQL support — **not yet hardened for production** (see [What's Missing / Roadmap](#whats-missing--roadmap)).

---

## Current Status — Phase 1

> **Phase 1 is feature-complete for local development. It is NOT production-ready.**
> See [What's Missing / Roadmap](#whats-missing--roadmap) for a prioritised list of gaps that must be resolved before exposing this service to any public traffic.

Phase 1 + Phase 2 (auth hardening) delivers:
- `POST /auth/signup`, `POST /auth/login` — JWT access token + refresh token
- `POST /auth/refresh` — token rotation (returns new pair, revokes old)
- `POST /auth/logout` — revokes refresh token; session ended
- `GET /actuator/health` — health check (no auth required)
- BCrypt password hashing — passwords are never stored or returned in plaintext
- CORS configured (`app.cors.allowed-origins`)
- 37 automated tests (unit + integration) — all passing
- H2 in-memory database (data is intentionally ephemeral in dev)

---

## Architecture

### Layers

```
controller  → thin HTTP layer, delegates to service
service     → business logic (signup/login), throws ResponseStatusException directly
repository  → Spring Data JPA interfaces
model       → JPA entities
dto         → request/response POJOs (Bean Validation annotations on requests)
security    → JwtUtil (token generation/parsing) + JwtAuthenticationFilter (per-request JWT check)
config      → SecurityConfig (filter chain, BCrypt bean)
```

### Request Flow

```
HTTP Request
  → JwtAuthenticationFilter   (extracts Bearer token, validates via JwtUtil, sets SecurityContext)
  → SecurityConfig filter chain   (permits /auth/**, blocks all others without valid JWT)
  → AuthController   (thin HTTP layer, @Valid, delegates to service)
  → AuthService   (email normalise, duplicate check, BCrypt, JWT issue)
  → UserRepository   (Spring Data JPA: findByEmail, existsByEmail)
  → H2 in-memory DB
```

### Key Design Decisions

- Email is normalised to `trim().toLowerCase()` in `AuthService` before any DB operation — not at the DTO or DB layer.
- JWT `sub` claim = user ID as a string (e.g. `"1"`); `email` is a separate custom claim.
- `SecurityContext` principal is the email string. There is no `UserDetails` or `UserDetailsService` — `JwtAuthenticationFilter` sets a bare `UsernamePasswordAuthenticationToken`.
- `ResponseStatusException` is thrown directly from `AuthService`. There is no custom exception hierarchy (yet — see Roadmap 2.2).

### File Structure

```
auth-service-java/
├── pom.xml
├── README.md
├── CONTRIBUTING.md
├── CODE_OF_CONDUCT.md
├── CLAUDE.md
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── .github/
│   ├── workflows/ci.yml
│   ├── PULL_REQUEST_TEMPLATE.md
│   └── ISSUE_TEMPLATE/
│       ├── bug_report.md
│       └── feature_request.md
├── docs/
│   ├── API_CONTRACT.md
│   ├── ARCHITECTURE.md
│   ├── DEPLOYMENT.md
│   ├── ENVIRONMENTS.md
│   ├── OBSERVABILITY.md
│   ├── RUNBOOK.md
│   └── ADR/           ← architectural decision records
└── src/
    ├── main/
    │   ├── java/com/authplatform/
    │   │   ├── config/        (SecurityConfig, OpenApiConfig)
    │   │   ├── controller/    (AuthController)
    │   │   ├── dto/           (request/response POJOs)
    │   │   ├── exception/     (GlobalExceptionHandler, ErrorResponse)
    │   │   ├── model/         (User, RefreshToken JPA entities)
    │   │   ├── repository/    (UserRepository, RefreshTokenRepository)
    │   │   ├── security/      (JwtUtil, JwtAuthenticationFilter)
    │   │   └── service/       (AuthService, RefreshTokenService)
    │   └── resources/
    │       ├── application.properties          ← dev (H2, Flyway off)
    │       ├── application-dev.properties      ← dev with H2 console
    │       ├── application-prod.properties     ← prod (PostgreSQL, Flyway on, Swagger off)
    │       └── db/migration/                  ← Flyway SQL migrations
    └── test/
```

---

## Quick Start

### Prerequisites

- Java 17+ — verify: `java -version`
- Maven 3.8+ — verify: `mvn -version`
- No database setup required — H2 runs in-memory automatically

### Steps

**1. Clone the repository**
```bash
git clone https://github.com/tirth6851/auth-service-java.git
cd auth-service-java
```

**2. Set the required JWT secret**
```bash
export JWT_SECRET="dev-secret-minimum-32-characters-1234"
```

**3. Run the application**
```bash
mvn spring-boot:run
```
App starts on `http://localhost:8080`.

**4. Explore the API in Swagger UI**

Open `http://localhost:8080/swagger-ui.html` in your browser. All endpoints are documented with request/response schemas and example values.

> Swagger UI is only available in the dev/default profile. It is disabled in production (`springdoc.swagger-ui.enabled=false`).

**5. Smoke-test signup**
```bash
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"str0ngPassw0rd!"}'
```
Expected: `{"token":"eyJ...","tokenType":"Bearer","refreshToken":"..."}`

**6. Use the token on a protected route**
```bash
curl http://localhost:8080/some-protected-endpoint \
  -H "Authorization: Bearer <paste-token-here>"
```
> A request to an unknown path with a valid token returns `404`; without a token returns `401`.

### H2 Console (dev only)

| Setting | Value |
|---------|-------|
| URL | `http://localhost:8080/h2-console` |
| JDBC URL | `jdbc:h2:mem:authdb` |
| Username | `sa` |
| Password | *(leave blank)* |

> **WARNING:** The H2 console is an unauthenticated SQL interface against the live database. It **must** be disabled (`spring.h2.console.enabled=false`) before running on any shared or internet-accessible host. See [Roadmap 1.2](#priority-1--critical-must-fix-before-any-public-use).

---

## API Reference

**Base URL:** `http://localhost:8080`

All requests require `Content-Type: application/json`. All responses are JSON.
All routes except `/auth/**` require `Authorization: Bearer <token>`.

**Interactive docs:** Run the app and open `http://localhost:8080/swagger-ui.html` — full request/response schemas, example values, and an Authorize button for testing protected routes. The raw OpenAPI spec is at `http://localhost:8080/v3/api-docs`.

---

### POST /auth/signup

Registers a new user and returns a JWT.

**Request body**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `email` | string | yes | Valid email format, not blank |
| `password` | string | yes | Minimum 8 characters, not blank |

**Example**
```http
POST /auth/signup HTTP/1.1
Content-Type: application/json

{
  "email": "alice@example.com",
  "password": "str0ngPassw0rd!"
}
```

```bash
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"str0ngPassw0rd!"}'
```

**Success — 200 OK**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJhbGljZUBleGFtcGxlLmNvbSIsImlhdCI6MTcxNTAwMDAwMCwiZXhwIjoxNzE1MDAzNjAwfQ.SIGNATURE",
  "tokenType": "Bearer"
}
```

**Error responses**

| Status | Condition | Notes |
|--------|-----------|-------|
| `400 Bad Request` | Invalid email format or password shorter than 8 chars | Body is Spring Boot's default error format until [Roadmap 2.2](#priority-2--important-should-have-before-real-users) is implemented |
| `409 Conflict` | Email already registered | Same Spring default format |

> **Email normalisation:** the email is trimmed and lowercased before storage and comparison. Submitting `" ALICE@EXAMPLE.COM "` is stored and matched as `"alice@example.com"`.

---

### POST /auth/login

Authenticates an existing user and returns a JWT.

**Request body** — same fields as signup.

**Example**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"str0ngPassw0rd!"}'
```

**Success — 200 OK** — same response shape as signup.

**Error responses**

| Status | Condition |
|--------|-----------|
| `400 Bad Request` | Blank field or invalid email format |
| `401 Unauthorized` | Email not found, or password does not match |

> **User enumeration protection:** the service returns the same `"Invalid credentials"` message whether the email is not found or the password is wrong. This is intentional — do not change it to be more specific.

---

### Using the JWT

| Property | Value |
|----------|-------|
| Algorithm | HS256 |
| `sub` claim | User ID as a string (e.g. `"1"`) |
| `email` claim | User's email address |
| Default lifetime | 1 hour (configurable via `app.jwt.expiration-ms`) |

Send the token in the `Authorization` header on all protected requests:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9....
```

---

## Configuration

**File:** `src/main/resources/application.properties`

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | HTTP port |
| `spring.datasource.url` | `jdbc:h2:mem:authdb` | H2 in-memory database URL |
| `spring.datasource.username` | `sa` | Database username |
| `spring.datasource.password` | *(empty)* | Database password |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema management strategy |
| `spring.jpa.show-sql` | `false` | Log SQL statements to console |
| `spring.h2.console.enabled` | `true` | Enable H2 web console |
| `spring.h2.console.path` | `/h2-console` | H2 console URL path |
| `app.jwt.secret` | `ReplaceThisWithAStrongSecretOfAtLeast32Chars!` | HS256 signing secret |
| `app.jwt.expiration-ms` | `3600000` | Token TTL in milliseconds (default: 1 hour) |

> **SECURITY — `app.jwt.secret` is hardcoded in a committed file.**
> This placeholder value **must** be replaced before any deployment. Use an environment variable:
> ```bash
> # Override at runtime
> APP_JWT_SECRET=your-very-long-random-secret-here mvn spring-boot:run
>
> # Or via -D flag
> mvn spring-boot:run -Dapp.jwt.secret=your-very-long-random-secret-here
> ```
> Generate a strong secret: `openssl rand -base64 48`
> See [Roadmap 1.1](#priority-1--critical-must-fix-before-any-public-use).

---

## Testing

**Run all tests**
```bash
mvn test
```

**Run a single test class**
```bash
mvn test -Dtest=AuthServiceTest
mvn test -Dtest=JwtUtilTest
mvn test -Dtest=AuthControllerIntegrationTest
```

**Test inventory**

| Class | Type | Count | What it covers |
|-------|------|-------|----------------|
| `JwtUtilTest` | Unit (no Spring context) | 8 | Token generation, claim extraction, validation, key-strength checks |
| `AuthServiceTest` | Unit (Mockito mocks) | 5 | Signup happy path, 409 on duplicate email, login happy path, wrong password 401, unknown email 401 |
| `AuthControllerIntegrationTest` | Integration (`@SpringBootTest` + MockMvc + H2) | 24 | Full HTTP contract: signup/login/refresh/logout, error codes, CORS, actuator health |

**Coverage gaps** (good first issues):
- `JwtAuthenticationFilter` has no isolated unit test
- No Testcontainers integration test against PostgreSQL (Flyway V2 migration is untested locally)
- Rate-limiting tests exist on `claude/rate-limit-login` branch but are not merged to `main` yet
- BCrypt encode/verify cycle tested via Mockito stubs only — no real round-trip test

---

## What's Done

### Auth Endpoints
- [x] `POST /auth/signup` — creates account; returns JWT + refresh token; `400` on invalid input, `409` on duplicate email
- [x] `POST /auth/login` — authenticates user; returns JWT + refresh token; `400` / `401` on bad credentials
- [x] `POST /auth/refresh` — rotates refresh token; returns new access + refresh token pair; `401` if expired/revoked
- [x] `POST /auth/logout` — revokes refresh token (idempotent); `204 No Content`
- [x] `GET /actuator/health` — health check; no auth required

### JWT & Tokens
- [x] HS256 JWT generation and parsing via JJWT 0.12.5
- [x] Token `sub` = user ID; custom `email` claim; configurable TTL (default 1 hour)
- [x] `JwtAuthenticationFilter` validates Bearer tokens on every request
- [x] Refresh tokens: UUID v4, stored as SHA-256 hash, 7-day TTL, rotated on every use
- [x] `JWT_SECRET` externalized to env var with fail-fast guard (`@PostConstruct` in `JwtUtil`)

### Security
- [x] BCrypt password hashing; passwords never stored or returned in plaintext
- [x] Email normalised (trimmed, lowercased) before storage and lookup
- [x] Stateless sessions (`SessionCreationPolicy.STATELESS`); CSRF disabled
- [x] CORS configured via `app.cors.allowed-origins` (default: localhost:3000, localhost:5173)
- [x] `401` (not `403`) returned for unauthenticated requests (`Http401UnauthorizedEntryPoint`)
- [x] Rate limiting on `POST /auth/login` — 10 attempts / 10 min / IP (on `claude/rate-limit-login`, pending merge)

### Data Layer
- [x] `User` entity + `RefreshToken` entity with FK to users
- [x] H2 in-memory for dev/test; PostgreSQL for production
- [x] Flyway migrations: V1 (users table), V2 (refresh_tokens table)

### Error Handling
- [x] `GlobalExceptionHandler` (`@RestControllerAdvice`) — consistent `{success, error, details}` shape
- [x] Validation errors include per-field `details` array
- [x] No internal details leaked in error responses

### OpenAPI / Swagger UI
- [x] `springdoc-openapi-starter-webmvc-ui` — Swagger UI at `/swagger-ui.html`, spec at `/v3/api-docs`
- [x] Bearer JWT `@SecurityScheme` — Authorize button in Swagger UI
- [x] All endpoints annotated with `@Operation`, `@ApiResponse`, `@Schema`
- [x] Disabled in production (`springdoc.swagger-ui.enabled=false`)

### Infrastructure
- [x] Multi-stage `Dockerfile` (Maven build + JRE runtime, non-root user)
- [x] `docker-compose.yml` with PostgreSQL 16 + app service and health checks
- [x] `.env.example` documenting all required variables
- [x] GitHub Actions CI — `mvn -B verify` on every push/PR to `main`

### Contributor Readiness
- [x] `CONTRIBUTING.md` — branch naming, coding rules, test expectations, ADR process
- [x] `CODE_OF_CONDUCT.md` — Contributor Covenant v2.1
- [x] GitHub issue templates (bug, feature) + PR template
- [x] `docs/DEPLOYMENT.md` — deployment guide, environment variables, production checklist
- [x] `docs/OBSERVABILITY.md` — logging baseline, monitoring signals, what to watch in prod

### Tests
- [x] 37 tests — all passing: 8 unit (`JwtUtilTest`), 5 unit (`AuthServiceTest`), 24 integration (`AuthControllerIntegrationTest`)
- [x] Integration tests use `@DirtiesContext`; `src/test/resources/application.properties` for H2 without env vars

---

## Roadmap

Items below are remaining gaps. Priority 1 must be resolved before exposing this service to real users. Items marked ✅ are complete.

---

### Priority 1 — Critical (must fix before any public use)

#### ✅ 1.1 JWT secret in environment variable — Complete
`JWT_SECRET` env var required at startup; fail-fast guard in `JwtUtil.init()`.

#### ✅ 1.2 H2 console disabled outside dev — Complete
`spring.h2.console.enabled=false` in default profile; enabled only via `dev` profile.

#### ✅ 1.3 PostgreSQL with Flyway — Complete
`application-prod.properties` uses `${POSTGRES_JDBC_URL}` etc. Flyway V1 migration applied on startup.

#### 1.4 Enforce HTTPS / TLS

Credentials and tokens travel in plaintext over HTTP.

**Recommended approach:** Terminate TLS at a load balancer or reverse proxy (nginx, Caddy, AWS ALB). Spring Boot app stays on HTTP internally. Add a `ForwardedHeaderFilter` bean to respect `X-Forwarded-Proto`.

---

### Priority 2 — Next sprint candidates

| Item | Effort | Label |
|------|--------|-------|
| Merge `claude/rate-limit-login` to `main` | Low | `good first issue` |
| `GET /auth/me` — user info from JWT claims | Low | `good first issue` |
| PostgreSQL Testcontainers integration test | Medium | `testing` |
| Account lockout after N failures | Medium | `enhancement` |
| Structured JSON logging (logstash-logback-encoder) | Low | `observability` |
| Audit log entity (signup/login/refresh events) | Medium | `observability` |
| HikariCP pool tuning in `application-prod.properties` | Low | `good first issue` |

---

### Priority 3 — Future

| Item | Notes |
|------|-------|
| Email verification | `User.isVerified` field exists; needs `spring-boot-starter-mail` |
| Password reset flow | `POST /auth/forgot-password` + `POST /auth/reset-password` |
| API versioning (`/api/v1/auth/`) | URI prefix; update SecurityConfig + tests |
| Postman / Bruno collection | Automate smoke tests; save JWT to env var |
| MCP adapter layer | See `docs/ADR/006-mcp-agent-auth-architecture.md` |
| RBAC / scope claims | `"scopes"` claim in JWT; `@PreAuthorize` on routes |
| API keys for agents | Long-lived `Authorization: ApiKey <key>` for non-interactive clients |

---

## Security Notes

**Never commit secrets**
- `app.jwt.secret` must never appear in a committed file. Use environment variables or a secrets manager.
- Generate a strong secret: `openssl rand -base64 48` (64 printable characters, well above the 32-char HS256 minimum)

**JWT algorithm (HS256 is symmetric)**
- Anyone who holds the signing secret can both verify and forge tokens. If you later need external services to verify tokens without forge capability, migrate to RS256. JJWT supports this without structural changes.

**Token revocation limitation**
- JWTs are stateless. A token remains valid until expiry even after "logout". Phase 1's only mitigation is the 1-hour default expiry. Full revocation requires a token blacklist (Redis is standard) or short-lived tokens with refresh token rotation — see Roadmap 2.3.

**Password policy**
- Current minimum: 8 characters. For production, consider enforcing additional complexity or using a library like `passay`. NIST SP 800-63B recommends length over complexity.

**BCrypt work factor**
- `BCryptPasswordEncoder()` defaults to strength factor 10 — a reasonable balance. Increasing to 12 gives stronger hashing at roughly 4× login latency.

**H2 console**
- Development-only. Disable (`spring.h2.console.enabled=false`) before running on any shared or internet-accessible host.

**Frame options / clickjacking**
- `SecurityConfig` currently calls `.frameOptions(f -> f.disable())` globally to allow H2 console iframes. When H2 is removed from the prod profile, restore clickjacking protection: `.frameOptions(f -> f.sameOrigin())`.

**User enumeration**
- Login returns the same `"Invalid credentials"` for both "email not found" and "wrong password". This is deliberate — do not change it to be more specific.

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the full guide — branch naming, coding rules, test expectations, and the ADR process for architectural proposals.

Quick checklist:
- Base branches off `main`; open a PR with the [PR template](.github/PULL_REQUEST_TEMPLATE.md)
- All tests must pass: `mvn test`
- New endpoints → update `docs/API_CONTRACT.md`; new config → update `docs/ENVIRONMENTS.md`
- No business logic in controllers; no JPA entities in HTTP responses
- `good first issue` labels mark beginner-friendly tasks

Found a security vulnerability? Email `tirth2093@gmail.com` — do not open a public issue.

---

## License

MIT License — see [LICENSE](LICENSE) for full text.

Copyright (c) 2026 Tirth Patel
