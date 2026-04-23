# Phase 1 — Spec

## Purpose

Stateless authentication API: email/password signup and login, BCrypt hashing, JWT issuance.

## Stack

Java 17, Spring Boot 3.2, Spring Web, Spring Data JPA, Spring Security, H2 (in-memory), JJWT 0.12.x, Maven.

## Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/signup` | None | Register new user, return JWT |
| POST | `/auth/login` | None | Authenticate, return JWT |

All other paths require `Authorization: Bearer <token>`.

## Request / Response

**Signup / Login request:**
```json
{ "email": "user@example.com", "password": "pass1234" }
```

**Success response (200):**
```json
{ "token": "<jwt>", "tokenType": "Bearer" }
```

**Error codes:**
- 400 — validation failure (bad email format, password < 8 chars)
- 401 — invalid credentials (login only)
- 409 — email already registered (signup only)

## Key Decisions

- Email normalised to `trim().toLowerCase()` in `AuthService`, not at DTO or DB layer.
- JWT subject = user ID (Long as string); `email` is a custom claim.
- `SecurityContext` principal = email string (not `UserDetails`).
- `ResponseStatusException` thrown directly from service — no custom exception classes.
- `UserDetailsService` is not implemented; Spring Security's user-loading contract is bypassed.

## Out of Scope (Phase 1)

Email verification, OTP, refresh tokens, roles, external auth providers, API keys, scheduled jobs.

## Configuration

- `app.jwt.secret` — HS256 key, must be ≥32 chars. Replace default before any shared deployment.
- `app.jwt.expiration-ms` — token TTL in ms (default 3600000 = 1 hour).
- H2 console: `http://localhost:8080/h2-console`, JDBC URL `jdbc:h2:mem:authdb`, user `sa`, no password.

## Known Limitations

- `isVerified` on `User` is always `false` — unused in Phase 1, placeholder for Phase 2.
- H2 is in-memory; all data is lost on restart.
- H2 console is unauthenticated — disable (`spring.h2.console.enabled=false`) in any non-local environment.
- JWT token failure is silent (invalid vs. expired tokens indistinguishable in logs).
