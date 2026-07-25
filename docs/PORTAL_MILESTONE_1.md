# Auth Portal — Milestone 1 (implementation-ready plan)

**Client for the Java Auth Service only.** Backend is the source of truth; the portal is a thin BFF client.
Planned 2026-07-08. Not yet implemented. Target: a new `web/` Next.js project (App Router + TS).

Backend facts this plan depends on (verified against source this session):
- `POST /auth/signup`, `POST /auth/login` → `200 {token, tokenType:"Bearer", refreshToken}`
- `POST /auth/refresh` → `200` new pair; reuse/expired/revoked → `401` (family revoked on reuse)
- `POST /auth/logout` (body `{refreshToken}`) → `204`, idempotent
- `GET /actuator/health` → `200 {status:"UP"}`
- JWT HS256, `sub`=userId, `email` claim; access TTL 15min prod / 1h dev; refresh 7d
- Uniform errors: `{success:false, error, details?}` — 400 (validation, `details[]`), 401 (invalid creds, enumeration-safe), 409 (duplicate email)
- **No** `/auth/me`, password reset, email verification, sessions API, RBAC.

---

## A. Milestone scope

**In:** signup, login, logout, protected app shell, minimal account page (email + userId), two clearly-marked placeholder pages.
**Out (do not build):** password reset, email verification, sessions/devices, RBAC/admin, any new backend feature. No calls to endpoints that don't exist.

## B. Web app architecture

**Stack:** Next.js App Router + TypeScript + Tailwind + shadcn/ui; `iron-session` (encrypted httpOnly cookie); `zod` (validation). Deploy on Vercel.

**Folder structure**
```
web/
├─ app/
│  ├─ layout.tsx  globals.css
│  ├─ page.tsx                         # / (public landing; redirect to /account if session)
│  ├─ (auth)/login/page.tsx            # /login
│  ├─ (auth)/signup/page.tsx           # /signup
│  ├─ (app)/layout.tsx                 # protected shell — getSession() or redirect
│  ├─ (app)/account/page.tsx           # /account
│  ├─ (app)/account/security/page.tsx  # placeholder (backend gap)
│  ├─ (app)/account/sessions/page.tsx  # placeholder (backend gap)
│  └─ api/auth/{signup,login,logout}/route.ts   # BFF route handlers
├─ lib/ session.ts  auth-api.ts  jwt.ts  validation.ts
├─ components/  (shadcn/ui, forms, nav)
├─ middleware.ts   # coarse gate on /account*
└─ .env.local      # AUTH_API_URL (server-only), SESSION_SECRET
```

**BFF route design** — the browser never calls the Java service directly; only these server routes do:
- `POST /api/auth/signup` → backend `/auth/signup` → create session → `{ok}` | mapped error
- `POST /api/auth/login`  → backend `/auth/login`  → create session → `{ok}` | mapped error
- `POST /api/auth/logout` → backend `/auth/logout {refreshToken}` → destroy cookie → `{ok}`
- internal helpers: `getSession()`, `getValidSession()` (silent refresh)

**Session/cookie strategy:** one `iron-session` cookie — **httpOnly, Secure, SameSite=Lax, encrypted** with `SESSION_SECRET`. Payload: `{ accessToken, refreshToken, accessTokenExpiresAt, userId, email }`. Refresh token lives **only** here (server-side, encrypted) — never in JS, localStorage, or sessionStorage. Because CORS is bypassed (same-origin to the BFF), the backend's CORS config is irrelevant.

**Protected routes:** `middleware.ts` redirects `/account*` to `/login?next=...` when the cookie is absent (coarse gate); the `(app)/layout.tsx` server component calls `getSession()` for the authoritative check and passes identity to children.

## C. Pages to build
- `/` — public landing; links to login/signup; if a session exists, redirect to `/account`.
- `/login` — email+password form → `POST /api/auth/login`.
- `/signup` — email+password form → `POST /api/auth/signup`.
- `/account` — shows `email` + `userId` from the session (M1) or `/auth/me` (optional, §G).
- **logout** — button/form → `POST /api/auth/logout`.
- `/account/security` — **placeholder**, clearly "Password management isn't available yet."
- `/account/sessions` — **placeholder**, clearly "Device/session management isn't available yet."
(Placeholders justified: they're the natural account sub-nav and map to known backend gaps; both must render an explicit "unavailable" state, no fake controls.)

## D. Data flow
- **Signup:** form (zod: valid email, password ≥ backend min) → `/api/auth/signup` → backend `/auth/signup` → decode `sub`/`email` from returned access token → `iron-session` save → redirect `/account`. Map 409→"email already registered", 400→field errors.
- **Login:** → backend `/auth/login` → save session → redirect `/account` (or `?next`). Map 401→generic "invalid email or password".
- **Silent refresh:** `getValidSession()` — if `accessTokenExpiresAt` within ~60s/passed, call backend `/auth/refresh` with the stored refresh token; on 200 rotate + re-save cookie; on 401 destroy cookie → treat as logged out. BFF is the single refresh holder → no multi-tab race.
- **Logout:** read session → backend `/auth/logout {refreshToken}` → destroy cookie → redirect `/login`. (Note: the current access JWT stays valid until ≤15-min expiry; there is no protected backend endpoint it can reach anyway.)
- **Account identity:** M1 decodes the access-token payload (base64 middle segment, decode-only — the token is server-held and came directly from the trusted service) to render email+userId. No signature verification in the BFF.

## E. Edge cases
- **Invalid credentials:** backend 401 → generic inline error, no email-vs-password distinction (match enumeration-safe backend).
- **Expired access token:** silent refresh; transparent.
- **Expired/revoked refresh token:** `/auth/refresh` 401 → destroy cookie → redirect `/login` with "session expired" notice.
- **Reuse-detection logout:** same handling as revoked (family already revoked server-side); destroy + re-login. Rare because the BFF is the sole refresh holder.
- **Direct nav to protected route while logged out:** middleware → 307 `/login?next=<path>`; after login redirect to `next` **only if it's a local path** (open-redirect guard).

## F. First 10 coding tasks
1. `create-next-app web` (TS, App Router, Tailwind, ESLint); add shadcn/ui; add `iron-session`, `zod`; create `.env.local` (`AUTH_API_URL`, `SESSION_SECRET`).
2. `lib/auth-api.ts` — server-only typed fetch wrappers for signup/login/logout/refresh against `AUTH_API_URL`; parse the uniform error shape.
3. `lib/session.ts` (iron-session config + get/save/destroy; session type) and `lib/jwt.ts` (decode-only `sub`/`email`/`exp`); `lib/validation.ts` (zod schemas mirroring backend rules).
4. `app/api/auth/login/route.ts` + `signup/route.ts` — validate → call backend → decode claims → save session → `{ok}`/error.
5. `app/api/auth/logout/route.ts` — read session → backend logout → destroy cookie.
6. `middleware.ts` — gate `/account*`; redirect to `/login?next=`; open-redirect guard.
7. `app/(auth)/login/page.tsx` + `/signup` — client forms, POST to BFF, generic error display, redirect on success.
8. `app/(app)/layout.tsx` — server `getSession()`; redirect if none; app shell nav + logout button.
9. `app/(app)/account/page.tsx` — render email+userId; add `/account/security` + `/account/sessions` placeholders marked unavailable.
10. `getValidSession()` silent refresh wired into the protected layout; `/` redirects to `/account` when a session exists; happy-path e2e (signup→account→logout).

## G. Optional single backend improvement
**`GET /auth/me` → `{userId, email}`** from the existing `AuthenticatedUser` principal. **Optional, not required:** M1's account page works by decoding the server-held access token in the BFF. Add `/auth/me` only when you want an authoritative/fresh identity source (e.g., after email changes) or prefer not to trust decoded-but-unverified claims. Scope: one controller method + one integration test (per project rules). Do **not** build it as part of M1 unless you choose to.
