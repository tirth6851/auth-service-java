# Session Handoff

**Last updated**: 2026-06-19 (Phase 2 — Rate Limiting on /auth/login)  
**Branch**: `claude/rate-limit-login` (off `main`)  
**Commit**: pending

---

## What Was Completed This Session

### Rate Limiting on POST /auth/login (branch `claude/rate-limit-login`)

**Policy:** 10 attempts per 10 minutes per client IP. Returns `429 Too Many Requests` with `Retry-After: <seconds>` header and standard `ErrorResponse` body when exceeded.

**Files changed:**

| File | Change |
|------|--------|
| `pom.xml` | Added `com.bucket4j:bucket4j-core:8.10.1` |
| `exception/RateLimitExceededException.java` | NEW — carries `retryAfterSeconds` field |
| `config/LoginRateLimitInterceptor.java` | NEW — HandlerInterceptor with per-IP `ConcurrentHashMap<String, Bucket>` |
| `config/WebConfig.java` | NEW — `WebMvcConfigurer` registering interceptor for `/auth/login` only |
| `exception/GlobalExceptionHandler.java` | Added `handleRateLimit()` — 429 + Retry-After header + ErrorResponse |
| `resources/application.properties` | Added `app.ratelimit.login.capacity=10` and `refill-period-seconds=600` |
| `test/resources/application.properties` | Override `capacity=3` for fast integration tests |
| `AuthControllerIntegrationTest.java` | 3 new tests (total: 30) |
| `docs/API_CONTRACT.md` | Rate Limiting section updated with policy, headers, config |
| `docs/ADR/005-rate-limiting-strategy.md` | NEW — design rationale |

**Key design decisions (see ADR 005):**

- `HandlerInterceptor` not `Filter` — exceptions propagate to `@RestControllerAdvice`, preserving error-shape consistency
- Keyed by `request.getRemoteAddr()` — XFF is attacker-controlled (rejected per advisor review)
- `Refill.intervally` (fixed window) — harder than greedy for brute-force protection
- Bucket map is an **instance field** not static — `DirtiesContext` resets it between tests correctly
- No `Thread.sleep` in tests (forbidden per ENGINEERING_STANDARDS)

**Tests added (3, total now 30):**
1. `login_underRateLimit_returns401NotRateLimited` — 2/3 attempts → 401, not 429
2. `login_returns429_whenRateLimitExceeded` — 4th attempt → 429
3. `login_returns429_withRetryAfterHeaderAndErrorBody` — 429 has `Retry-After` + standard error body

---

## Branch State

| Branch | Contents | Status |
|--------|----------|--------|
| `main` | Phase 1 complete + all prior security (clean) | Stable |
| `claude/security-hardening-round2` | Security fixes round 2 | Pushed — needs PR → merge |
| `claude/auth-me` | GET /auth/me (off security branch) | Pushed — needs PR after security merges |
| `claude/rate-limit-login` | Rate limiting on /auth/login (off main) | Needs push + CI + PR |

**Merge order for existing branches:**
1. `claude/security-hardening-round2` → main (no deps)
2. `claude/auth-me` → main (after security merges — branched off it)
3. `claude/rate-limit-login` → main (off main, independent — can merge any time)

---

## Outstanding Risks or Blockers

- ⚠️ **mvn test not run locally** — Maven not in PATH. CI will verify on push.
- ⚠️ **gh CLI token expired** — Cannot create PRs from CLI. See next steps below.
- ⚠️ `/auth/signup` not yet rate-limited — noted in PROJECT_BACKLOG.md
- ⚠️ In a horizontally-scaled deployment, rate limit is per-instance (not shared). A Redis-backed Bucket4j extension would be needed for shared-state enforcement.

---

## Exact First Steps for Next Session

### Priority 1: Push and open PR for this branch
```bash
git push origin claude/rate-limit-login

# After gh auth login:
gh pr create --base main --head claude/rate-limit-login \
  --title "feat: rate limit POST /auth/login (Bucket4j, 10/10min/IP)" \
  --body "Adds in-process Bucket4j rate limiting to /auth/login. 429 + Retry-After header. Keyed by remoteAddr. 3 new integration tests (30 total). See ADR 005."
```

### Priority 2: Merge all pending branches (in order)
```bash
gh auth login   # if token expired

# 1. Security hardening
gh pr create --base main --head claude/security-hardening-round2 \
  --title "security: harden Dockerfile, frame options, and docker-compose"

# 2. GET /auth/me (after security merges)
gh pr create --base main --head claude/auth-me \
  --title "feat: GET /auth/me — authenticated user profile endpoint"

# 3. Rate limiting (independent, can merge any time)
# See Priority 1 above
```

### Priority 3: Next Phase 2 feature candidates
From PROJECT_BACKLOG.md (Medium Priority):
- **Refresh tokens** (POST /auth/refresh + POST /auth/logout) — bigger, multi-file, needs DB migration
- **Audit logging** — structured auth event log (signup, login success/failure, token rejection)
- **Rate limiting /auth/signup** — extend LoginRateLimitInterceptor or create a separate one

---

## Demo Flow (after merge)

```bash
# 1. Login normally
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"str0ngPassw0rd!"}'
# → 200 OK with token

# 2. Trigger rate limit (repeat 10+ times with any email/password)
for i in {1..11}; do
  curl -s -o /dev/null -w "Attempt $i: HTTP %{http_code}\n" \
    -X POST http://localhost:8080/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"attacker@example.com","password":"guess'$i'"}'
done
# → First 10: HTTP 401 (wrong credentials)
# → 11th+: HTTP 429 with Retry-After header

# 3. Check the 429 response body
curl -i -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"attacker@example.com","password":"guess"}'
# → HTTP/1.1 429 Too Many Requests
# → Retry-After: 543
# → {"success":false,"error":"Too many login attempts. Please try again later."}
```

---

## Handoff Validation Checklist

- [x] Bucket4j dependency added with verified Maven coordinate (`com.bucket4j:bucket4j-core:8.10.1`)
- [x] Rate limiter keyed by `remoteAddr` (not XFF)
- [x] `Retry-After` header set with exact nanos-to-refill from Bucket4j probe
- [x] Standard `ErrorResponse` shape preserved (no custom JSON in filter/interceptor)
- [x] Interceptor bucket map is instance field (not static) — DirtiesContext resets it
- [x] Test capacity=3 in test properties; no Thread.sleep in tests
- [x] 3 new integration tests — under limit, over limit, header+body
- [x] `docs/API_CONTRACT.md` updated with rate limiting policy
- [x] `docs/ADR/005-rate-limiting-strategy.md` created
- [x] `docs/PROJECT_BACKLOG.md` updated (rate limiting marked complete, signup gap noted)
- [x] `docs/PROJECT_PROGRESS.md` updated (Milestone 8)
- [x] HANDOFF.md updated

---

*Last updated: 2026-06-19 (Phase 2 — Rate Limiting). Next session: push branch, open PR, then next Phase 2 feature.*
