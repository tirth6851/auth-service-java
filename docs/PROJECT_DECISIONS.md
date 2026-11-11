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

## D-002 — Spring Boot 3.2 / Spring Security 6 *(upgraded to Boot 3.5.16 — see D-012)*

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
**Consequences:** Emails are case-insensitively unique both in practice (normalised before every write/query) and at the DB layer — `V1__create_users_table.sql` includes a `LOWER(email)` unique index (`idx_users_email_lower`) as a defense-in-depth constraint. Lookup queries remain exact-match since input is always pre-normalised; the index is not currently referenced by any `@Query`.

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

---

## D-011 — Open-Source Positioning, License, and Contribution Terms

**Date:** 2026-11-10
**Decision:** (1) Position the project as a **self-hostable auth service**, not a reusable library. (2) **Keep the MIT license** and add a **Developer Certificate of Origin** (`Signed-off-by`, enforced by `.github/workflows/dco.yml`) instead of a CLA. (3) Publish a **3-business-day first-response target** for issues, PRs, and security reports. (4) Make factual claims only: an explicit "unaudited" notice, and no comparative or niche claims until an overlap check against comparable projects is done.
**Reason:** The codebase is a runnable Spring Boot service plus web portal, so Maven Central publishing and adapter-module advice (written for libraries) does not apply. MIT is permissive and well understood; a DCO gives clearer provenance without CLA administration a solo maintainer cannot staff. The response target is one a single maintainer can sustain; publishing a tighter one risks silence, which is the main driver of lost contributors. Overclaiming is a larger risk than underclaiming for authentication software.
**Alternatives considered:** Apache-2.0 plus DCO (explicit patent grant; needed later for CNCF, not needed now); extracting an auth library (large refactor, competes with established libraries); a 2-business-day target (riskier during exams).
**Consequences:** `SECURITY.md`, `CONTRIBUTING.md`, PR template, and `.github/` automation (Dependabot, CodeQL, Scorecard, DCO) were added. Relicensing to Apache-2.0 stays possible later because the maintainer holds all copyright today, but gets harder once outside contributions land. Full plan and checklist: `docs/OPEN_SOURCE_LAUNCH.md`.

---

## D-012 — Upgrade to Spring Boot 3.5.16 (and JJWT 0.13.0, springdoc 2.10.0)

**Date:** 2026-11-10
**Decision:** Move from Spring Boot 3.2.5 to **3.5.16** (the newest 3.5.x), JJWT 0.12.5 → **0.13.0**, springdoc 2.5.0 → **2.10.0** (built on Boot 3.5.16), and let Spring Boot manage the Testcontainers version (1.21.4) instead of pinning 1.19.7. Add the **`flyway-database-postgresql`** module. Java stays at 17.
**Reason:** Boot 3.2 is out of open-source support, so it no longer receives security fixes. This supersedes Dependabot PR #35, but done deliberately, because it touches the stack that handles tokens.
**What changed that matters:**
- **Flyway 9.22.3 → 11.7.2.** Since Flyway 10, PostgreSQL support is a separate module. Without `flyway-database-postgresql`, `prod` migrations fail at startup. The H2 suite runs with Flyway off and **cannot catch this**; only the Testcontainers test can, so CI now runs it (`Postgres Integration` job).
- **Hibernate 6.4.4 → 6.6.53, Spring Security 6.2.4 → 6.5.11, H2 2.2.224 → 2.3.232, PostgreSQL driver 42.6.2 → 42.7.11, Jackson 2.15.4 → 2.21.4, Tomcat 10.1.20 → 10.1.55** (all Boot-managed; versions taken from the Boot 3.2.5 and 3.5.16 BOMs).
- **JJWT 0.13.0** differs from 0.12.7 only by one constructor's visibility; the code uses only HMAC sign and verify, so there is no behavioural change here. The key-strength and tamper tests pass unchanged.
- **Testcontainers 2.0** (Dependabot's suggestion) was deliberately not taken: it renames artifacts and is a separate migration.
**Verification:** clean `mvn clean test` (72/72) with `spring-boot-properties-migrator` temporarily enabled (no renamed or removed properties reported); the packaged app was started and the full API flow exercised by hand (signup, `/auth/me`, tampered and missing tokens, refresh rotation, reuse detection and family revocation, logout, validation errors, Swagger UI, 404 vs 401); the real-Postgres test, extended to cover refresh rotation and reuse detection, runs in CI.
**Known leftovers:** `Bandwidth.classic` / `Refill` in `RateLimitInterceptor` are deprecated in Bucket4j 8.10 (unrelated to this upgrade). The Dockerfile's `maven:3.8-openjdk-17` build image was not exercised or changed.
**Consequences:** Boot 3.5 is the line to stay on until 3.5 reaches end of support; Boot 4 is a separate, larger migration. Dependabot ignores major-version bumps (D-011), so Spring Boot minor/patch updates in the 3.5 line still arrive.
