# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application
mvn spring-boot:run

# Build (compile + test)
mvn package

# Run tests only
mvn test

# Run a single test class
mvn test -Dtest=AuthServiceTest

# Compile without tests
mvn compile -DskipTests
```

App starts at `http://localhost:8080`. H2 console at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:authdb`, user: `sa`, no password).

## Architecture

Layered Spring Boot 3.2 / Java 17 app. Package root: `com.authplatform`.

```
controller  → thin HTTP layer, delegates to service
service     → business logic (signup/login), throws ResponseStatusException directly
repository  → Spring Data JPA interfaces
model       → JPA entities
dto         → request/response POJOs (Bean Validation annotations on requests)
security    → JwtUtil (token generation/parsing) + JwtAuthenticationFilter (per-request JWT check)
config      → SecurityConfig (filter chain, BCrypt bean)
```

**Security flow:** Every request passes through `JwtAuthenticationFilter` before reaching controllers. The filter extracts the `Authorization: Bearer <token>` header, validates it via `JwtUtil`, then sets a `UsernamePasswordAuthenticationToken` in the `SecurityContext`. `/auth/**` and `/h2-console/**` are permit-all; all other paths require authentication.

**JWT structure:** HS256, subject = user ID (Long as string), custom `email` claim, expiry controlled by `app.jwt.expiration-ms`. The signing key is derived from `app.jwt.secret` (must be ≥32 chars).

**Database:** H2 in-memory (`authdb`), schema managed by `spring.jpa.hibernate.ddl-auto=update`. Data is lost on restart — by design for Phase 1.

## Configuration

Key properties in `application.properties`:
- `app.jwt.secret` — replace with a strong secret (≥32 chars) in any real deployment
- `app.jwt.expiration-ms` — token TTL in ms (default `3600000` = 1 hour)

## Coding rules

- No business logic in controllers — controllers are HTTP-only, all logic lives in `AuthService`
- Keep methods small; avoid unnecessary abstractions
- Use DTOs for all request/response objects; never expose JPA entities directly
- Minimal comments only where logic is non-obvious
- No placeholder code, no pseudo-code, no speculative abstractions

## Phase scope

This is Phase 1 only. Intentionally absent: refresh tokens, email verification/OTP, role-based authorization, API keys, scheduled jobs, external auth providers. Do not add these unless explicitly requested.

## Completion checklist

Before calling any task done, verify:
1. Project compiles (`mvn compile`)
2. App runs (`mvn spring-boot:run`)
3. Signup returns a JWT
4. Login returns a JWT
5. Invalid credentials return `401`
6. Duplicate email returns `409`
7. Passwords are BCrypt-hashed, never stored or returned in plaintext
8. README is updated if behavior changed
9. Any assumptions are documented
