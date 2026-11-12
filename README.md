# auth-service-java

[![CI](https://github.com/tirth6851/auth-service-java/actions/workflows/ci.yml/badge.svg)](https://github.com/tirth6851/auth-service-java/actions/workflows/ci.yml)
![Java 17](https://img.shields.io/badge/Java-17-blue?logo=openjdk)
![Spring Boot 3.5.16](https://img.shields.io/badge/Spring%20Boot-3.5.16-brightgreen?logo=springboot)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

A small, readable, self-hostable authentication service built with Spring Boot 3.5 and Java 17:
email and password signup and login, short-lived JWT access tokens, rotating refresh tokens with reuse
detection, and per-IP rate limiting. It ships with an optional Next.js web portal in [`web/`](web/).

> **Unaudited.** This project has not had an independent security review. Read the code and the
> [security notes](#security-notes) before exposing it to real users. To report a vulnerability, follow
> [SECURITY.md](SECURITY.md); please don't open a public issue.

## What it does

| Endpoint | Auth | Purpose |
|----------|------|---------|
| `POST /auth/signup` | none | Create an account; returns an access token and a refresh token |
| `POST /auth/login` | none | Authenticate; returns an access token and a refresh token |
| `POST /auth/refresh` | refresh token | Rotate: returns a new pair and revokes the old refresh token |
| `POST /auth/logout` | refresh token | Revoke a refresh token (idempotent, `204`) |
| `GET /auth/me` | Bearer token | Current user's profile (`id`, `email`, `verified`, `createdAt`) |
| `GET /actuator/health` | none | Liveness check |

- **Passwords:** BCrypt; never stored, returned, or logged in plaintext.
- **Access tokens:** HS256 JWT, `sub` = user ID, plus an `email` claim. 1 hour by default, **15 minutes**
  in the `prod` profile.
- **Refresh tokens:** random UUID, stored only as a SHA-256 hash, 7-day TTL, rotated on every use.
  Presenting an already-rotated or revoked token revokes all of that user's active refresh tokens
  (reuse detection), and concurrent rotation is guarded by optimistic locking.
- **Rate limiting:** `POST /auth/login` and `POST /auth/signup` each allow 10 attempts per 10 minutes per
  IP, in separate buckets; over the limit returns `429` with a `Retry-After` header.
- **Login does not reveal whether an email exists:** unknown email and wrong password return the same
  `401 Invalid credentials`.
- **Errors:** one consistent JSON shape; no stack traces or internals leaked.
- **Storage:** H2 in memory for dev and tests; PostgreSQL with Flyway migrations in `prod`.
- **Ops:** multi-stage non-root `Dockerfile`, `docker-compose.yml` with PostgreSQL 16, `render.yaml`
  blueprint, Swagger UI in dev (disabled in `prod`).

Full request and response specs: [`docs/API_CONTRACT.md`](docs/API_CONTRACT.md).

## Quick start

### Run it locally (Maven, in-memory H2)

Requires JDK 17+ and Maven 3.8+. No database setup.

```bash
git clone https://github.com/tirth6851/auth-service-java.git
cd auth-service-java

export JWT_SECRET="dev-secret-minimum-32-characters-1234"   # any 32+ character string in dev
mvn spring-boot:run
```

The service starts on `http://localhost:8080`. Swagger UI is at `/swagger-ui.html` and the OpenAPI spec at
`/v3/api-docs`.

Try it:

```bash
# 1. Sign up
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"str0ngPassw0rd!"}'
# {"token":"eyJ...","tokenType":"Bearer","refreshToken":"550e8400-..."}

# 2. Call a protected endpoint with the access token
curl http://localhost:8080/auth/me -H "Authorization: Bearer <token>"
# {"id":1,"email":"alice@example.com","verified":false,"createdAt":"..."}

# 3. Rotate the refresh token
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'

# 4. Log out
curl -X POST http://localhost:8080/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<new refreshToken>"}'
```

To use the in-memory database console during development, run with `--spring.profiles.active=dev` and open
`/h2-console` (JDBC URL `jdbc:h2:mem:authdb`, user `sa`, blank password). It is disabled by default and
must never be enabled on a shared or internet-facing host.

### Run it with Docker Compose (PostgreSQL)

```bash
cp .env.example .env
# edit .env and set JWT_SECRET to a real secret:  openssl rand -base64 48
docker compose up --build
```

This starts PostgreSQL 16 and the service with the `prod` profile (Flyway migrations on, Swagger off).

### Run the web portal

The portal in [`web/`](web/) is a Next.js app that talks to the service through server-side routes only;
the browser never sees the refresh token. Setup is in [`web/README.md`](web/README.md).

### Deploy

[`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) covers a two-service deploy (backend plus portal) on Render,
and the repository includes a [`render.yaml`](render.yaml) blueprint. You must supply `JWT_SECRET`,
`SESSION_SECRET`, and the JDBC URL yourself; nothing secret is committed.

## Configuration

Secrets and environment-specific values come from environment variables. Defaults live in
`src/main/resources/application*.properties`.

| Setting | Default | Notes |
|---------|---------|-------|
| `JWT_SECRET` | **required** | HS256 signing secret, minimum 32 characters. The app fails fast at startup without it. Generate with `openssl rand -base64 48`. |
| `SPRING_PROFILES_ACTIVE` | none | `dev` enables the H2 console; `prod` uses PostgreSQL and Flyway. |
| `POSTGRES_JDBC_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | none | Required in the `prod` profile. |
| `HIKARI_MAX_POOL_SIZE` | `5` (`prod` only) | Maximum HikariCP connections per application instance. |
| `HIKARI_CONNECTION_TIMEOUT_MS` | `20000` (`prod` only) | Maximum wait for a connection (milliseconds). |
| `HIKARI_MAX_LIFETIME_MS` | `1500000` (`prod` only) | Maximum pooled connection lifetime (milliseconds). |
| `app.jwt.expiration-ms` | `3600000` (1 h); `900000` (15 min) in `prod` | Access token lifetime. |
| `app.refresh-token.ttl-ms` | `604800000` (7 days) | Refresh token lifetime. |
| `app.cors.allowed-origins` | `http://localhost:3000,http://localhost:5173` | Comma-separated explicit origins. |
| `app.ratelimit.login.capacity` / `.refill-period-seconds` | `10` / `600` | Login attempts per IP per window. |
| `app.ratelimit.signup.capacity` / `.refill-period-seconds` | `10` / `600` | Signup attempts per IP per window. |

More detail: [`docs/ENVIRONMENTS.md`](docs/ENVIRONMENTS.md).

## Architecture

```
controller  → thin HTTP layer, delegates to service
service     → business logic (AuthService, RefreshTokenService)
repository  → Spring Data JPA interfaces
model       → JPA entities (User, RefreshToken)
dto         → request/response types; entities are never exposed over HTTP
security    → JwtUtil, JwtAuthenticationFilter, AuthenticatedUser principal
config      → SecurityConfig (filter chain, CORS), rate-limit interceptor, OpenAPI
exception   → GlobalExceptionHandler and the consistent error shape
```

```
HTTP request
  → RateLimitInterceptor        (login and signup only; 429 when over the limit)
  → JwtAuthenticationFilter     (validates the Bearer token, sets AuthenticatedUser)
  → SecurityConfig              (explicit public path list; everything else requires a token)
  → AuthController              (HTTP only, @Valid, delegates)
  → AuthService / RefreshTokenService
  → UserRepository / RefreshTokenRepository → H2 (dev) or PostgreSQL (prod)
```

Key decisions, each recorded as an ADR in [`docs/ADR/`](docs/ADR/): JWT subject is the user ID, thin
controllers, stateless JWT auth, DTOs not entities, refresh-token design, rate limiting. Longer design
notes: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Testing

```bash
mvn test                              # 72 tests, in-memory H2, no Docker needed
mvn test -Dtest=AuthServiceTest       # one class
mvn clean test jacoco:report          # coverage: target/site/jacoco/index.html
mvn -Pintegration-test verify         # also runs the Postgres Testcontainers test (needs Docker)
```

| Class | Type | Tests |
|-------|------|-------|
| `AuthControllerIntegrationTest` | `@SpringBootTest` + MockMvc + H2: full HTTP contract, rate limits, CORS | 40 |
| `JwtUtilTest` | Unit | 8 |
| `RefreshTokenServiceTest` | Unit (Mockito) | 8 |
| `JwtAuthenticationFilterTest` | Unit | 7 |
| `AuthServiceTest` | Unit (Mockito) | 5 |
| `GlobalExceptionHandlerTest` | Unit | 2 |
| `RefreshTokenOptimisticLockTest` | Repository | 2 |
| `PostgresFlywayIntegrationTest` | Testcontainers, Docker-gated, excluded from `mvn test` | 3 |

The web portal has no automated test suite yet; CI does lint and build it (`Web Build` job).

## Security notes

- **Unaudited.** See the notice at the top and [SECURITY.md](SECURITY.md).
- **HS256 is symmetric.** Anyone holding `JWT_SECRET` can verify and forge tokens. If other services need to
  verify tokens without being able to mint them, move to RS256; JJWT supports it.
- **Access tokens can't be revoked before they expire.** Logout revokes the refresh token only. The
  15-minute production lifetime limits the exposure.
- **No TLS in the app.** Terminate TLS at a reverse proxy or load balancer.
- **Rate limiting is per instance and keyed on the socket address.** It uses `request.getRemoteAddr()`,
  deliberately not `X-Forwarded-For` (which clients can forge). Behind a reverse proxy that address is the
  proxy's, so all clients would share one bucket, and several replicas multiply the limit. A shared store
  such as Redis and trusted-proxy handling are not implemented.
- **Password policy is minimal:** 8 characters minimum. NIST SP 800-63B favours length over composition rules.
- **BCrypt strength** is Spring's default (10). Raising it to 12 costs roughly 4× login latency.
- **H2 console** is dev-only and off by default.
- **Login is deliberately non-enumerating.** Don't make its error messages more specific.

## Not implemented

These are real gaps, not hidden features:

- Password reset and email verification (`verified` is always `false`)
- Account lockout, audit logging, and token revocation or denylist
- Sessions and devices API, and roles or scopes (RBAC)
- Shared (Redis-backed) rate limiting
- Automated tests for the web portal

Planned and candidate work is tracked in [`docs/PROJECT_BACKLOG.md`](docs/PROJECT_BACKLOG.md).

## Contributing

Contributions are welcome, including docs and tests. Start with [CONTRIBUTING.md](CONTRIBUTING.md): it
covers picking a [`good first issue`](https://github.com/tirth6851/auth-service-java/labels/good%20first%20issue),
the DCO sign-off (`git commit -s`), coding rules, and the ADR process. I aim to respond to every issue and
pull request within 3 business days. Everyone taking part is expected to follow the
[Code of Conduct](CODE_OF_CONDUCT.md).

## License

[MIT](LICENSE). Copyright (c) 2026 Tirth Patel.
