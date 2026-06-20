# auth-service-java

![Java 17](https://img.shields.io/badge/Java-17-blue?logo=openjdk)
![Spring Boot 3.2.5](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?logo=springboot)
![Tests](https://img.shields.io/badge/tests-37%20passing-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)
![CI](https://img.shields.io/badge/CI-not%20configured-lightgrey)

A stateless JWT authentication REST API built with Spring Boot 3.2 and Java 17. Provides email/password signup and login with BCrypt password hashing and HS256 JWT issuance. Phase 1 is a fully functional local/dev foundation — **it is not yet hardened for production** (see [What's Missing / Roadmap](#whats-missing--roadmap)).

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
├── CLAUDE.md
├── docs/
│   ├── spec.md
│   ├── todo.md
│   └── done-criteria.md
└── src/
    ├── main/
    │   ├── java/com/authplatform/
    │   │   ├── AuthPlatformApplication.java
    │   │   ├── config/
    │   │   │   └── SecurityConfig.java
    │   │   ├── controller/
    │   │   │   └── AuthController.java
    │   │   ├── dto/
    │   │   │   ├── AuthResponse.java
    │   │   │   ├── LoginRequest.java
    │   │   │   └── SignupRequest.java
    │   │   ├── model/
    │   │   │   └── User.java
    │   │   ├── repository/
    │   │   │   └── UserRepository.java
    │   │   ├── security/
    │   │   │   ├── JwtAuthenticationFilter.java
    │   │   │   └── JwtUtil.java
    │   │   └── service/
    │   │       └── AuthService.java
    │   └── resources/
    │       └── application.properties
    └── test/java/com/authplatform/
        ├── controller/
        │   └── AuthControllerIntegrationTest.java
        ├── security/
        │   └── JwtUtilTest.java
        └── service/
            └── AuthServiceTest.java
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

**2. Run the application**
```bash
mvn spring-boot:run
```
App starts on `http://localhost:8080`.

**3. Smoke-test signup**
```bash
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"str0ngPassw0rd!"}'
```
Expected: `{"token":"eyJ...","tokenType":"Bearer"}`

**4. Use the token on a protected route**
```bash
curl http://localhost:8080/some-protected-endpoint \
  -H "Authorization: Bearer <paste-token-here>"
```
> Note: no protected endpoints exist yet in Phase 1. A request to an unknown path returns `404`, which confirms the JWT filter accepted the token. A request without a token returns `401`.

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
| `JwtUtilTest` | Unit (no Spring context) | 5 | Token generation, claim extraction, validation, garbage-input rejection |
| `AuthServiceTest` | Unit (Mockito mocks) | 5 | Signup happy path, 409 on duplicate email, login happy path, wrong password 401, unknown email 401 |
| `AuthControllerIntegrationTest` | Integration (`@SpringBootTest` + MockMvc + H2) | 7 | Full HTTP contract: status codes, JSON shape, duplicate email, invalid inputs, bad credentials |

**Coverage gaps** (known, for future contributors):
- `JwtAuthenticationFilter` has no isolated unit test
- No test verifies that a request to a protected route without a token returns `401`
- No test verifies that an expired token is rejected
- BCrypt encode/verify cycle is only tested via Mockito stubs in `AuthServiceTest`, not a real round-trip

---

## What's Done — Phase 1

### Core Authentication
- [x] `POST /auth/signup` — registers user, returns JWT; `400` on invalid input, `409` on duplicate email
- [x] `POST /auth/login` — authenticates user, returns JWT; `400` on invalid input, `401` on wrong credentials
- [x] BCrypt password hashing via Spring Security `PasswordEncoder`
- [x] Email normalised to lowercase and trimmed before storage and lookup
- [x] Passwords never stored or returned in plaintext

### JWT
- [x] HS256 JWT generation and parsing via JJWT 0.12.5
- [x] Token `sub` = user ID string; custom `email` claim included
- [x] Configurable token expiry (default 1 hour via `app.jwt.expiration-ms`)
- [x] `JwtAuthenticationFilter` validates Bearer tokens on every request and populates `SecurityContext`

### Data Layer
- [x] `User` entity: `id` (auto PK), `email` (unique, not null), `passwordHash`, `isVerified` (always `false` in Phase 1), `createdAt` (set on persist via `@PrePersist`)
- [x] `UserRepository` with `findByEmail` and `existsByEmail`
- [x] H2 in-memory database; schema managed by Hibernate `ddl-auto=update`

### Security Configuration
- [x] Stateless session policy (`SessionCreationPolicy.STATELESS`)
- [x] CSRF disabled (appropriate for a stateless JSON API)
- [x] `/auth/**` and `/h2-console/**` are permit-all; all other routes require a valid JWT
- [x] Bean validation on DTOs (`@Email`, `@NotBlank`, `@Size(min=8)`)

### Tests
- [x] 17 tests — all passing: 5 unit (`JwtUtilTest`), 5 unit (`AuthServiceTest`), 7 integration (`AuthControllerIntegrationTest`)
- [x] Integration tests use `@DirtiesContext` to reset H2 state between each test method

### Documentation
- [x] `README.md` with run instructions, API reference, curl examples
- [x] `docs/spec.md` — design decisions and out-of-scope items
- [x] `docs/done-criteria.md` — Phase 1 acceptance checklist
- [x] `CLAUDE.md` — AI-assisted development guidance

---

## What's Missing / Roadmap

Work through items in order. Nothing in Phase 1 is safe for public internet traffic. Resolve all Priority 1 items before exposing this service to anyone.

---

### Priority 1 — Critical (must fix before any public use)

#### 1.1 Move JWT secret to an environment variable
**Problem:** `app.jwt.secret` is hardcoded in `application.properties`, which is committed to version control. Anyone with repo access can forge tokens.

**Fix:**
- Remove the value from `application.properties`; replace with `app.jwt.secret=${JWT_SECRET}`
- Create `src/main/resources/application-prod.properties` with no secrets
- Add a `@PostConstruct` guard in `JwtUtil` that throws `IllegalStateException` if the secret is still the placeholder or shorter than 32 characters
- Set `JWT_SECRET` via environment variable, CI secret, or a secrets manager (AWS Secrets Manager, HashiCorp Vault, GitHub Secrets)

**Files:** `src/main/resources/application.properties`, `src/main/java/com/authplatform/security/JwtUtil.java`, new `src/main/resources/application-prod.properties`

---

#### 1.2 Disable H2 console outside local development
**Problem:** `spring.h2.console.enabled=true` is on by default. The H2 console is an unauthenticated web UI that allows arbitrary SQL against the live database — a critical vulnerability on any shared host.

**Fix:**
- Set `spring.h2.console.enabled=false` in `application-prod.properties`
- Keep `true` only in the dev `application.properties`
- Update `SecurityConfig` to make the H2 permit-all rule conditional on the dev profile, or remove it from the prod filter chain

**Files:** `src/main/resources/application.properties`, `src/main/resources/application-prod.properties`, `src/main/java/com/authplatform/config/SecurityConfig.java`

---

#### 1.3 Replace H2 with a persistent database (PostgreSQL)
**Problem:** H2 in-memory means all user accounts are lost on every application restart. The service is unusable for any real scenario.

**Fix:**
- Add `org.postgresql:postgresql` (runtime scope) to `pom.xml`
- In `application-prod.properties`: `spring.datasource.url=${DB_URL}`, `spring.datasource.username=${DB_USER}`, `spring.datasource.password=${DB_PASSWORD}`
- Keep H2 in `application.properties` for local dev
- Switch `spring.jpa.hibernate.ddl-auto` to `validate` in prod (pair with Flyway — see 1.4)

**Dependencies:** `org.postgresql:postgresql`

---

#### 1.4 Add database migrations (Flyway)
**Problem:** `ddl-auto=update` lets Hibernate silently alter the production schema, which cannot be audited or rolled back.

**Fix:**
- Add `org.flywaydb:flyway-core` to `pom.xml`
- Create `src/main/resources/db/migration/V1__create_users_table.sql` with explicit `CREATE TABLE users` DDL matching the `User` entity
- Set `spring.jpa.hibernate.ddl-auto=validate` in prod
- Set `spring.flyway.enabled=false` in dev `application.properties` (H2 auto-DDL is fine for local dev)

**Files to create:** `src/main/resources/db/migration/V1__create_users_table.sql`

---

#### 1.5 Enforce HTTPS / TLS
**Problem:** Credentials and tokens travel in plaintext over HTTP.

**Recommended fix (option A — TLS at the proxy):** Terminate TLS at a load balancer or reverse proxy (nginx, Caddy, AWS ALB). The Spring Boot app stays on HTTP internally. Add a `ForwardedHeaderFilter` bean so Spring sees the correct scheme from `X-Forwarded-Proto`.

**Alternative fix (option B — Tomcat SSL):** Configure `server.ssl.*` properties in `application-prod.properties` with a keystore. Add an HTTP→HTTPS redirect in `SecurityConfig`.

---

### Priority 2 — Important (should have before real users)

#### 2.1 Rate limiting on `/auth/login`
**Problem:** `/auth/login` accepts unlimited requests. An attacker can brute-force passwords with no friction.

**Fix:** Add `com.bucket4j:bucket4j-core` or use Resilience4j `@RateLimiter`. Apply a limit of ~10 attempts per IP per 10 minutes on `/auth/login`. Return `429 Too Many Requests` with a `Retry-After` header on limit exceeded.

**Files to create:** `src/main/java/com/authplatform/config/RateLimitConfig.java` (or a `OncePerRequestFilter`)

---

#### 2.2 Structured, consistent error responses
**Problem:** `ResponseStatusException` produces Spring Boot's default error body, which is unpredictable across versions and hard for API clients to parse reliably.

**Fix:** Create a `@RestControllerAdvice` `GlobalExceptionHandler`. Handle `ResponseStatusException`, `MethodArgumentNotValidException` (returns all field errors in an `"errors"` array), and a catch-all `Exception`. Consistent response shape:
```json
{ "status": 400, "error": "Bad Request", "message": "...", "timestamp": "..." }
```

**Files to create:** `src/main/java/com/authplatform/exception/GlobalExceptionHandler.java`, `src/main/java/com/authplatform/dto/ErrorResponse.java`

---

#### 2.3 Refresh tokens + `/auth/logout`
**Problem:** Access tokens expire in 1 hour with no renewal path. Users must re-submit credentials to continue — unworkable for any app with a UI.

**Fix:**
- Add `RefreshToken` entity: `id`, `token` (UUID), `userId` (FK), `expiresAt` (7 days), `createdAt`, `revokedAt`
- `POST /auth/refresh` — accepts `{"refreshToken":"..."}`, validates (not expired, not revoked), issues new access JWT + rotated refresh token
- `POST /auth/logout` — accepts refresh token, sets `revokedAt`, immediately invalidates session
- Modify signup/login responses to include `refreshToken` alongside the access `token`

**Files to create:** `RefreshToken.java`, `RefreshTokenRepository.java`, `RefreshTokenService.java`
**Files to modify:** `AuthController.java`, `AuthResponse.java`

---

#### 2.4 CORS configuration
**Problem:** Browser-based frontends (SPAs) cannot call the API from a different origin. There is no CORS configuration.

**Fix:** Add a `CorsConfigurationSource` bean in `SecurityConfig`. Source allowed origins from `app.cors.allowed-origins` (default in dev: `http://localhost:3000,http://localhost:5173`; must be set explicitly in prod). Register the `CorsFilter` before the JWT filter. Do not use `*` as `allowedOrigins` when `allowCredentials` is `true`.

**Files:** `src/main/java/com/authplatform/config/SecurityConfig.java`, `application.properties`, `application-prod.properties`

---

#### 2.5 Health check endpoint
**Problem:** No `/health` endpoint. Load balancers, Kubernetes liveness probes, and Docker Compose health checks all need one.

**Fix:** Add `spring-boot-starter-actuator` to `pom.xml`. In prod, expose only health: `management.endpoints.web.exposure.include=health`, `management.endpoint.health.show-details=never`. Permit `/actuator/health` in `SecurityConfig`.

---

#### 2.6 Dockerfile + docker-compose.yml
**Problem:** No container support. Most deployment targets (ECS, GKE, Render, Railway) require a container image.

**Dockerfile:** Multi-stage build — Stage 1: `maven:3.9-eclipse-temurin-17` to build the fat JAR. Stage 2: `eclipse-temurin:17-jre-alpine` to run it. Non-root user. Expose port 8080.

**docker-compose.yml:** Two services — `db` (postgres:16-alpine with persistent volume and healthcheck) and `app` (built from Dockerfile, depends on `db`, env vars for `JWT_SECRET`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `SPRING_PROFILES_ACTIVE=prod`).

**Files to create:** `Dockerfile`, `docker-compose.yml`, `.dockerignore`

---

#### 2.7 GitHub Actions CI pipeline
**Problem:** No automated build-and-test on push or pull request. Regressions can be merged silently.

**Fix:** Create `.github/workflows/ci.yml`. Trigger on `push` to `main` and `pull_request`. Job: checkout → set up JDK 17 → `mvn verify` → upload test reports as artifact. Optionally add a `docker-build` job (build image, don't push) to catch Dockerfile regressions.

**Files to create:** `.github/workflows/ci.yml`

---

### Priority 3 — Nice to Have

#### 3.1 Email verification
`User.isVerified` exists and is always `false`. To activate it: on signup, generate a secure token, store it in an `EmailVerificationToken` entity, send a verification email. `GET /auth/verify?token=...` marks the user verified. Optionally block login for unverified users.

**Dependencies:** `spring-boot-starter-mail` or an email provider SDK (SendGrid, Resend, Postmark)

---

#### 3.2 Password reset flow
`POST /auth/forgot-password` — always returns `200` regardless of whether email exists (prevents enumeration). If found, store a short-lived `PasswordResetToken` and send a reset link. `POST /auth/reset-password` — validates token, hashes new password, invalidates token.

---

#### 3.3 Account lockout after repeated failures
Add `failedLoginAttempts` (int) and `lockedUntil` (Instant, nullable) to `User`. After 5 consecutive failures, lock the account for 15 minutes, returning `423 Locked`. Reset counts on successful login. Requires a Flyway migration for the new columns.

---

#### 3.4 API versioning
Move all endpoints to `/api/v1/auth/**`. Update `SecurityConfig` permit-all rules and all tests. Decide on a versioning strategy (URI prefix shown here for simplicity).

---

#### 3.5 OpenAPI / Swagger UI
Add `springdoc-openapi-starter-webmvc-ui`. Annotate `AuthController`, DTOs, and errors with `@Operation`, `@ApiResponse`, `@Schema`. UI at `http://localhost:8080/swagger-ui.html`. Disable the UI in prod (`springdoc.swagger-ui.enabled=false`) or protect it.

---

#### 3.6 Structured logging
Add `src/main/resources/logback-spring.xml`. Dev: coloured console at DEBUG. Prod: JSON output via `logstash-logback-encoder`, INFO level, include `traceId`. Log each login attempt (success/failure) at INFO with email and result — never log passwords or raw JWTs.

---

#### 3.7 Audit logging
`AuditLog` entity: `userId` (nullable), `action` (enum: `SIGNUP`, `LOGIN_SUCCESS`, `LOGIN_FAILURE`, `PASSWORD_RESET`, `EMAIL_VERIFIED`), `ipAddress`, `userAgent`, `createdAt`. Write a record in `AuthService` after each action. Critical for incident response.

---

#### 3.8 Postman / Bruno collection
Create `docs/auth-service.postman_collection.json` with requests for Signup, Login, JWT-protected route, and Refresh token (placeholder). Use `{{base_url}}` environment variable. Add a test script on Login that auto-saves the returned token to `{{jwt_token}}`.

---

#### 3.9 HikariCP connection pool configuration
When switching to PostgreSQL, configure HikariCP in `application-prod.properties`:
```
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
```

---

## Next Steps

The recommended implementation order — each sprint builds on the previous one.

```
Sprint 0 — Security (do before sharing with anyone)
  1. Move JWT secret to environment variable        → Roadmap 1.1
  2. Disable H2 console in prod profile             → Roadmap 1.2
  3. Create application-prod.properties             → enables 1.1, 1.2, 1.3

Sprint 1 — Make It Real (do before any real user data)
  4. Add PostgreSQL + Flyway V1 migration           → Roadmap 1.3 + 1.4
  5. Add Dockerfile + docker-compose.yml            → Roadmap 2.6  (depends on step 4)
  6. Add GitHub Actions CI                          → Roadmap 2.7
  7. Add /actuator/health endpoint                  → Roadmap 2.5

Sprint 2 — Harden Auth
  8. Global exception handler + ErrorResponse DTO   → Roadmap 2.2
  9. Rate limiting on /auth/login                   → Roadmap 2.1
  10. CORS configuration                            → Roadmap 2.4
  11. Refresh tokens + /auth/logout                 → Roadmap 2.3

Sprint 3 — Features
  12. Email verification flow                       → Roadmap 3.1
  13. Password reset flow                           → Roadmap 3.2
  14. Account lockout (5 attempts → 15 min lock)   → Roadmap 3.3
  15. OpenAPI / Swagger UI                          → Roadmap 3.5
  16. API versioning (/api/v1/)                     → Roadmap 3.4

Sprint 4 — Observability
  17. Structured JSON logging (logback-spring.xml)  → Roadmap 3.6
  18. Audit logging entity + service                → Roadmap 3.7
  19. Postman / Bruno collection                    → Roadmap 3.8
  20. HikariCP pool configuration                   → Roadmap 3.9
```

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

- Fork the repository and create a feature branch from `main`
- All changes must include tests; run `mvn test` and confirm it passes before opening a PR
- No business logic in controllers — all logic lives in `AuthService`
- Use DTOs for all HTTP payloads; never expose JPA entities directly
- Minimal comments — only where the *why* is non-obvious
- Reference `CLAUDE.md` for full coding rules and `docs/spec.md` for design decisions already made

---

## License

MIT License — see [LICENSE](LICENSE) for full text.

Copyright (c) 2026 Tirth Patel
