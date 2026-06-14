# Project Decisions

Record of every significant architectural or engineering decision made during development.

---

## D-001 — JWT Authentication (not sessions or API keys)

**Date:** 2026-04 (Phase 1 design)
**Decision:** Use HS256 JWTs issued on signup/login; verified per-request via a custom Spring Security filter.
**Reason:** Stateless, portable, standard for REST APIs; no server-side session store required; fits agent-native use cases where tokens can be passed programmatically.
**Alternatives considered:**
- HTTP sessions with cookies — stateful, bad for microservices and AI agents
- API keys — simpler but no built-in expiry or claims; Phase 2 consideration
- OAuth2 — overkill for Phase 1; planned for later phases
**Consequences:** Every protected request incurs JWT parse cost (negligible at this scale). Token revocation requires a denylist (not implemented in Phase 1).

---

## D-002 — Spring Boot 3.2 / Spring Security 6

**Date:** 2026-04
**Decision:** Use Spring Boot 3.2.5 (GA), which pulls in Spring Security 6.
**Reason:** Latest stable LTS-compatible release; Security 6 has the updated `HttpSecurity` lambda DSL and `SecurityFilterChain` bean model.
**Alternatives considered:** Spring Boot 2.x — older API, uses deprecated `WebSecurityConfigurerAdapter`; would require migration later.
**Consequences:** Spring Security 6 changed the default `AuthenticationEntryPoint` to `Http403ForbiddenEntryPoint` when no `httpBasic`/`formLogin` is configured — this caused the 403 bug fixed in PR #6. Must always configure `exceptionHandling` explicitly.

---

## D-003 — Java 17

**Date:** 2026-04
**Decision:** Target Java 17 (LTS).
**Reason:** LTS release, widely supported, enables text blocks, records (future use), pattern matching. Required by Spring Boot 3.x.
**Alternatives considered:** Java 21 — newer LTS, but less universal in hosted environments at time of decision.
**Consequences:** Byte Buddy (used by Mockito) required `--add-opens` / experimental mode flag in Surefire config for compatibility with Java 25 runtime (see `pom.xml` `argLine`).

---

## D-004 — H2 In-Memory Database (Phase 1 only)

**Date:** 2026-04
**Decision:** Use H2 in-memory database with `ddl-auto=update`.
**Reason:** Zero-config, no external dependency, fast test startup. Acceptable for Phase 1 where persistence across restarts is explicitly out of scope.
**Alternatives considered:** PostgreSQL — production target but adds Docker/setup complexity for Phase 1.
**Consequences:** All data lost on restart (documented). H2 console unauthenticated — must be disabled in any shared or production environment. PostgreSQL migration is a Phase 2 priority.

---

## D-005 — Environment-Based JWT Secret

**Date:** 2026-04 (PR #3/4)
**Decision:** `app.jwt.secret` reads from `${JWT_SECRET}` environment variable with no default. `JwtUtil.init()` throws `IllegalStateException` on blank, placeholder, or short (<32 char) secrets.
**Reason:** Prevents accidental deployment with a weak or committed secret. Fail-fast is safer than silent fallback.
**Alternatives considered:** Default placeholder in `application.properties` — rejected because it could ship to production unnoticed.
**Consequences:** Developers must set `JWT_SECRET` before running locally. Integration tests need `src/test/resources/application.properties` to override this (added in PR #6).

---

## D-006 — Dev/Prod Profile Separation for H2 Console

**Date:** 2026-04 (PR #3)
**Decision:** `spring.h2.console.enabled=false` in default profile; enabled only when `spring.profiles.active=dev`.
**Reason:** H2 console is unauthenticated — exposing it in production would be a critical security hole.
**Alternatives considered:** Always enable it and rely on firewall — rejected as defense-in-depth failure.
**Consequences:** Developers must pass `-Dspring.profiles.active=dev` (or set env var) to access the H2 console during local development.

---

## D-007 — Fail-Fast JWT Validation in JwtUtil

**Date:** 2026-04 (PR #4)
**Decision:** `JwtUtil` validates secret at `@PostConstruct` time (not lazily on first token operation). Three checks: non-blank, not the placeholder string, minimum 32 chars.
**Reason:** Immediate startup failure is better than silent token-generation with a weak key that passes all unit tests but fails in production.
**Alternatives considered:** Lazy validation on first use — rejected; errors surface too late and only under load.
**Consequences:** Application will not start without a valid `JWT_SECRET`. This is intentional.

---

## D-008 — Email Normalisation in Service Layer

**Date:** 2026-04
**Decision:** `email.trim().toLowerCase()` applied in `AuthService`, not in the DTO or at the database layer.
**Reason:** Keeps DTOs as pure data carriers; avoids coupling normalisation logic to persistence. Service layer owns business rules.
**Alternatives considered:** DTO setter normalisation — mixing concerns. DB `LOWER()` index — adds DB-specific logic.
**Consequences:** Emails are case-insensitively unique in practice even though the DB column has no `LOWER()` unique index. Future migration to PostgreSQL should add a functional unique index.

---

## D-009 — SecurityContext Principal = Email String (not UserDetails)

**Date:** 2026-04
**Decision:** `JwtAuthenticationFilter` sets `UsernamePasswordAuthenticationToken` with the email string as principal, not a `UserDetails` object.
**Reason:** Phase 1 has no role-based authorization; loading a full `UserDetails` per request would require a DB hit for no benefit.
**Alternatives considered:** Implement `UserDetailsService` — standard Spring Security approach but adds unnecessary complexity and a DB query per request.
**Consequences:** Code calling `SecurityContextHolder.getContext().getAuthentication().getPrincipal()` gets a `String` (email), not a `UserDetails`. When roles are added, this decision should be revisited.

---

## D-010 — Http401UnauthorizedEntryPoint (explicit AuthenticationEntryPoint)

**Date:** 2026-06-14 (PR #6)
**Decision:** Create `Http401UnauthorizedEntryPoint` implementing `AuthenticationEntryPoint` and wire it via `.exceptionHandling(eh -> eh.authenticationEntryPoint(...))` in `SecurityConfig`.
**Reason:** Spring Security 6 defaults to `Http403ForbiddenEntryPoint` when no `httpBasic()`/`formLogin()` is configured. RFC 7235 requires 401 (with `WWW-Authenticate`) for unauthenticated access; 403 is for authorization failures by authenticated users.
**Alternatives considered:** `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)` built-in — simpler but less explicit; a named class makes the intent obvious in the filter chain.
**Consequences:** Missing/invalid/expired JWTs now correctly return 401. Future role-based access denials (authenticated user, insufficient permission) will return 403 via the `AccessDeniedHandler` path, which is the correct semantic split.
