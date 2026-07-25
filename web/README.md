# Auth Portal (web)

Thin BFF web client for the [Java auth service](../README.md). Milestone 1: signup, login,
logout, a protected account shell, and two clearly-marked placeholder pages for backend
features that don't exist yet (password management, device/session management).

Full design and scope: [`../docs/PORTAL_MILESTONE_1.md`](../docs/PORTAL_MILESTONE_1.md).

## Architecture

- Next.js App Router + TypeScript + Tailwind + shadcn/ui.
- The browser never calls the Java backend directly — only server-side BFF route handlers under
  `app/api/auth/*` do (`lib/auth-api.ts`).
- Session state (access token, refresh token, decoded identity) lives in one `iron-session`
  encrypted, httpOnly cookie (`lib/session.ts`). The refresh token never reaches browser
  JS/localStorage.
- `proxy.ts` (Next.js 16's replacement for `middleware.ts`) gates `/account/*`: redirects to
  `/login` if there's no session cookie, and transparently rotates the access token via
  `POST /auth/refresh` when it's near expiry — this is the only place allowed to both read and
  rewrite the session cookie within one request. Server Components (`lib/auth.ts`'s
  `getIdentity()`) only ever *read* the session; they never call `/auth/refresh` themselves.

## Setup

```bash
npm install
cp .env.example .env.local   # then fill in SESSION_SECRET (see comment in the file)
npm run dev
```

Requires the backend running locally (`mvn spring-boot:run` from the repo root, with
`JWT_SECRET` set) at the URL configured in `AUTH_API_URL`.

## Environment variables

| Variable | Required | Description |
|----------|----------|-------------|
| `AUTH_API_URL` | Yes | Base URL of the Java auth service. Server-only, never exposed to the browser. |
| `SESSION_SECRET` | Yes | 32+ char random secret encrypting the session cookie. Generate with `node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"`. Rotating it logs out all sessions. |

## Commands

```bash
npm run dev      # start dev server on :3000
npm run build     # production build (also type-checks)
npm run start     # run the production build
npm run lint      # eslint
```
