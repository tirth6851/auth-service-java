# ADR 007 — Rate Limiting Strategy for /auth/login

**Date:** 2026-06-19  
**Status:** Accepted  
**Branch:** `claude/rate-limit-login`

---

## Context

`POST /auth/login` is publicly accessible and accepts any email/password combination.
Without rate limiting an attacker can try unlimited passwords against a known email address.
The platform needs brute-force protection without introducing Redis or any external infrastructure.

---

## Decision

**Bucket4j in-process token bucket**, keyed by client IP address (`HttpServletRequest.getRemoteAddr()`).

**Policy:** 10 attempts per 10 minutes per IP.  
**Response when exceeded:** `429 Too Many Requests` with `Retry-After: <seconds>` header.  
**Scope:** `/auth/login` only. `/auth/signup` and all other endpoints are unaffected.

---

## Rationale

### Library: Bucket4j

- Pure in-process, zero external dependencies (no Redis, no DB tables)
- Token bucket algorithm gives smooth rate limiting with natural burst tolerance
- `ConsumptionProbe.getNanosToWaitForRefill()` gives exact `Retry-After` value
- Maven coordinate: `com.bucket4j:bucket4j-core:8.10.1`

### Placement: HandlerInterceptor (not Filter)

A Spring MVC `HandlerInterceptor` was chosen over a `javax.servlet.Filter` because:
- Interceptors run inside the `DispatcherServlet` and exceptions propagate to `@RestControllerAdvice`
- The existing `GlobalExceptionHandler` can produce the standard `ErrorResponse` JSON shape + `Retry-After` header
- A filter would require hand-writing JSON to the response, breaking error-shape consistency

Registered via `WebMvcConfigurer.addInterceptors()` scoped to `/auth/login` only.

### Keying: Remote Address (not X-Forwarded-For)

`request.getRemoteAddr()` is used, not `X-Forwarded-For`.

`X-Forwarded-For` is an arbitrary HTTP header that any client can forge. Keying on it would allow an attacker to bypass the limiter by sending a different XFF value per request, effectively giving themselves unlimited buckets.

**Trade-off accepted:** Users behind a shared NAT egress IP (corporate offices, mobile carriers) share a bucket. With a limit of 10 per 10 minutes, this is unlikely to affect legitimate users in practice. If needed, a proper proxy-aware strategy (`server.forward-headers-strategy=framework`) can be added as a separate change.

### Refill strategy: `Refill.intervally`

`Refill.intervally(10, Duration.ofMinutes(10))` — all 10 tokens are replenished together after the full 10-minute window expires. This is a fixed-window model.

Alternative `Refill.greedy` would have allowed 1 token per minute continuously, meaning an attacker can indefinitely try 1 login per minute — weaker protection against slow brute-force. Fixed-window is the stricter and more predictable choice for a login endpoint.

---

## Consequences

- **New production configuration** required: `app.ratelimit.login.capacity` and `app.ratelimit.login.refill-period-seconds` (defaults: 10, 600).
- **Test configuration** overrides capacity to 3 so integration tests can trigger 429 in 4 requests without sleeping.
- `/auth/signup` is **not** rate-limited by this change. A separate ADR should decide signup rate limiting.
- No DB migration required.
- No Redis or external service required.
- In a horizontally scaled deployment, each instance maintains its own in-memory bucket. This is acceptable: an attacker hitting multiple instances would see a higher effective limit. A future Redis-backed rate limiter can replace this if shared-state enforcement is required.

---

## Alternatives Rejected

| Option | Reason rejected |
|--------|----------------|
| Redis-backed Bucket4j | Adds external infrastructure dependency — violates "no external infra" constraint for Phase 2 |
| Spring Security's built-in rate limiting | Not available in Spring Security 6.x as a first-class feature |
| API gateway (nginx limit_req) | Out of scope for application-layer implementation; no gateway in this stack |
| Servlet Filter | Exceptions don't flow to `@RestControllerAdvice`; would require hand-written JSON error body |
