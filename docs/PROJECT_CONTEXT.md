# Project Context

## Overview

`auth-service-java` is a stateless authentication REST API built with Spring Boot 3.2 / Java 17. It handles email/password registration and login, issues HS256 JWTs, and enforces token-based access on all non-auth routes. It is **Phase 1** of a longer roadmap toward an **Agent-Native Authentication Platform** — a backend designed to serve both human clients and AI agents.

## Long-Term Vision

Turn this service into a production-grade, agent-aware authentication platform:
- MCP (Model Context Protocol) server so AI agents can authenticate calls autonomously
- Structured audit trail for agent actions
- Role/permission model that expresses machine-readable capability scopes
- Pluggable external auth providers (OAuth2, SAML)
- Multi-tenant support

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2 (Spring MVC, Spring Security 6, Spring Data JPA) |
| JWT library | JJWT 0.12.x (HS256) |
| Database | H2 in-memory (Phase 1 only) |
| Build | Maven (spring-boot-maven-plugin) |
| Testing | JUnit 5, Mockito, Spring Boot Test, MockMvc, Spring Security Test |

## Package Structure

```
com.authplatform
├── controller    — thin HTTP layer, delegates to service
├── service       — business logic (signup/login), throws ResponseStatusException
├── repository    — Spring Data JPA interfaces
├── model         — JPA entities (User)
├── dto           — request/response POJOs with Bean Validation
├── security      — JwtUtil, JwtAuthenticationFilter, Http401UnauthorizedEntryPoint
└── config        — SecurityConfig (filter chain, BCrypt bean)
```

## Key Endpoints

| Method | Path | Auth | Response |
|--------|------|------|----------|
| POST | `/auth/signup` | None | 200 `{token, tokenType}` / 400 / 409 |
| POST | `/auth/login` | None | 200 `{token, tokenType}` / 400 / 401 |
| * | `/auth/**` | None | permit-all |
| * | `/h2-console/**` | None | permit-all (dev only) |
| * | everything else | Bearer JWT required | 401 if missing/invalid |

## Security Model

**Filter chain (per request):**
1. `JwtAuthenticationFilter` (before `UsernamePasswordAuthenticationFilter`) extracts `Authorization: Bearer <token>`, validates via `JwtUtil`, sets `UsernamePasswordAuthenticationToken` in `SecurityContext` with email as principal.
2. Spring Security checks `authorizeHttpRequests` rules.
3. Unauthenticated access → `Http401UnauthorizedEntryPoint` → **401 Unauthorized**.
4. Future role checks → 403 Forbidden (not yet implemented).

**JWT structure:** HS256, subject = user ID (Long as string), custom `email` claim, 1-hour TTL by default. Signing key derived from `app.jwt.secret` (≥32 chars, injected via `JWT_SECRET` env var in production, overridden by `src/test/resources/application.properties` in tests).

**Passwords:** BCrypt-hashed, never stored or returned in plaintext.

**Session policy:** `STATELESS` — no cookies, no HTTP sessions.

## Configuration

`src/main/resources/application.properties`:
- `app.jwt.secret=${JWT_SECRET}` — must be ≥32 chars; set `JWT_SECRET` env var before running
- `app.jwt.expiration-ms=3600000` — 1 hour
- `spring.h2.console.enabled=false` — disabled by default; enabled only via `spring.profiles.active=dev`

`src/test/resources/application.properties`:
- Overrides `app.jwt.secret` with a hardcoded test value so integration tests run without `JWT_SECRET` set

## Testing Strategy

Three-layer test suite (23 tests total as of 2026-06-14):

| Class | Type | Count | What it covers |
|-------|------|-------|----------------|
| `JwtUtilTest` | Unit | 8 | Token generation, parsing, validation, expiry |
| `AuthServiceTest` | Unit (Mockito) | 5 | Signup/login logic, email normalisation, conflict/credential errors |
| `AuthControllerIntegrationTest` | Integration (MockMvc) | 10 | Full HTTP contracts, security filter, status codes |

Run: `mvn test`
