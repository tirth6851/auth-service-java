# Agent-Native Authentication Platform — Roadmap

> **Status:** Design only. No feature code yet. This document defines the
> architecture before implementation. Phases are sequenced for safe delivery.
> **Audience:** maintainers deciding how to evolve this service from a
> human-facing auth API into an agent-callable platform.

---

## 1. Why this document exists

The service today is a solid, conventional JWT auth API. The differentiating
goal is to make it **agent-native**: an AI agent (Claude, Cursor, a custom
LLM agent) can perform auth operations directly through a machine-friendly
interface, governed by least-privilege permissions and a complete audit trail.

```
Human ──"create an account for John"──▶ Agent (Claude/Cursor/…)
                                          │
                                          ▼
                                     MCP Server  ── tool: create_user(...)
                                          │
                                          ▼  HTTPS + agent credential
                                  auth-service-java  ── /agent/v1/users
                                          │
                                          ▼
                                     User / RefreshToken / AuditLog
```

The human-facing surface (`/auth/**`) does **not** change. The agent surface
is **additive**.

---

## 2. Current state (grounded — what we build on)

| Area | Today | Reuse for agent layer |
|------|-------|------------------------|
| Endpoints | `POST /auth/{signup,login,refresh,logout}` | Agent endpoints are a new, separate group |
| Access token | HS256 JWT, 1h, `sub`=userId, `email` claim, no authorities | Extend principal to carry agent identity + scopes |
| Refresh token | Opaque UUID, **SHA-256 hashed at rest**, 7d, rotated, revocable | **Same hashing/revocation pattern reused for API keys** |
| Authorization | **None** — JWT filter sets empty authorities; every authed request is equally privileged | Greenfield: build scope-based authz |
| API keys | **None** | New `AgentClient` entity |
| Audit | **None** (only `createdAt`/`revokedAt` lifecycle stamps) | New `AuditLog` entity |
| Error shape | `GlobalExceptionHandler` + `ErrorResponse` (`success`, `error`, `details`) | Agent endpoints reuse this shape |
| DB | dev: H2 + `ddl-auto=update`; prod: PostgreSQL + Flyway (`classpath:db/migration`, `ddl-auto=validate`) | New tables need **Flyway migrations** for prod |
| Docs/OpenAPI | springdoc 2.5.0, Swagger UI, Bearer configured | Document agent endpoints + add API-key security scheme |
| User entity | `id, email, passwordHash, isVerified, createdAt` — **no status/enabled field** | `disable/enable_user` requires a new column + migration |

> **Honest gaps that turn into work items:**
> - There is no `User.status` (or `enabled`) field. `disable_user` / `enable_user`
>   cannot be built without adding one (+ Flyway migration for prod).
> - There is no authority/role concept anywhere. Phase C is fully greenfield.
> - `find_user` / `search` need new `UserRepository` query methods.

---

## 3. Design principles

1. **Additive, not invasive.** `/auth/**` stays exactly as-is. Agent surface
   lives under `/agent/v1/**` and is permit-listed separately.
2. **Reuse existing primitives.** SHA-256 token hashing (already used for
   refresh tokens), BCrypt, stateless filter chain, `ErrorResponse`.
3. **Least privilege.** An agent credential carries an explicit, finite set of
   scopes. No scope ⇒ no access. Wildcard (`*`) is opt-in and audited.
4. **Every agent action is audited.** Non-negotiable; it is the strongest part
   of the story and the safety net for autonomous operations.
5. **Fail closed.** Unknown scope, revoked key, expired key ⇒ 401/403, logged.

---

## 4. Phase A — Agent Access (how an agent authenticates)

An agent is not a human; it should not log in with email/password. It needs a
machine credential.

### Options considered

| Option | Pros | Cons | Verdict |
|--------|------|------|---------|
| **Scoped API key** | Simple; revocable; matches existing hashed-token pattern; easy for MCP server to hold | Long-lived secret to manage | **Recommended (Phase A)** |
| Agent JWT | Stateless | Who issues/rotates it? Reinvents API-key issuance | Defer |
| OAuth2 client-credentials | Industry standard; short-lived tokens | Heaviest; needs token endpoint + client registry | **Phase A.2 / scale-up path** |

### Recommendation: scoped API keys, hashed at rest

- New entity **`AgentClient`**: `id`, `name`, `keyHash` (SHA-256 of raw key —
  reuse the refresh-token hashing util), `scopes` (set), `createdAt`,
  `lastUsedAt`, `revokedAt`.
- Raw key shown **once** at creation (format e.g. `ak_live_<random>`), never stored.
- Presented by clients as `Authorization: Bearer ak_live_…` **or** a dedicated
  `X-Agent-Key` header (decision in §8).
- New **`AgentAuthenticationFilter`** (sibling to `JwtAuthenticationFilter`):
  resolves the key → `AgentClient` → builds an `Authentication` whose
  authorities are the client's scopes. Rejects revoked/expired keys via the
  existing `Http401UnauthorizedEntryPoint`.
- Bootstrap: a one-time admin key seeded via env/migration so the first agent
  can be provisioned.

---

## 5. Phase B — Agent Tools (capabilities)

Each tool maps 1:1 to an HTTP endpoint under `/agent/v1` and to a required
scope (Phase C). MCP tools (Phase D) call these.

| Tool | Endpoint | Scope | Notes / new work |
|------|----------|-------|------------------|
| `create_user` | `POST /agent/v1/users` | `users:write` | Reuses signup logic in `AuthService` |
| `find_user` | `GET /agent/v1/users?email=` | `users:read` | New `UserRepository` query |
| `search_users` | `GET /agent/v1/users?…filters` | `users:read` | Pagination; criteria (createdAt, verified, status) |
| `disable_user` | `POST /agent/v1/users/{id}/disable` | `users:write` | **Needs new `User.status`/`enabled` field + migration** |
| `enable_user` | `POST /agent/v1/users/{id}/enable` | `users:write` | Same field |
| `reset_password` | `POST /agent/v1/users/{id}/reset-password` | `users:reset_password` | Generates temp credential / forces rotation |
| `list_sessions` | `GET /agent/v1/users/{id}/sessions` | `sessions:read` | Reads `refresh_tokens` for user |
| `revoke_session` | `POST /agent/v1/sessions/{id}/revoke` | `sessions:revoke` | Sets `revokedAt` (logic already exists) |

> A disabled user must also be blocked at login and have active sessions
> revoked — that coupling is part of the `disable_user` work item.

---

## 6. Phase C — Permission Model

Greenfield. Scope-based, enforced from the agent's `Authentication` authorities.

### Scope catalog (initial)

```
users:read           users:write          users:reset_password
sessions:read        sessions:revoke      *  (superuser, audited)
```

### Example agent definitions

```yaml
support-agent:
  - users:read
  - sessions:read
  - sessions:revoke

provisioning-agent:
  - users:read
  - users:write
  - users:reset_password

admin-agent:
  - "*"
```

### Enforcement

- Scopes become Spring Security authorities at authentication time.
- Guard endpoints with method security (`@PreAuthorize("hasAuthority('users:write')")`)
  or a small explicit check — prefer `@PreAuthorize` for declarative clarity.
- `*` short-circuits to allow, but the audit entry records that wildcard was used.

---

## 7. Phase D — MCP Integration

A dedicated **MCP server** (separate process; language TBD in §8) exposes the
Phase B tools and calls this service over HTTPS using a Phase-A agent key.

```
Claude ──tool call──▶ MCP server ──Bearer ak_live_…──▶ /agent/v1/* ──▶ DB
```

- Tools mirror Phase B exactly (`create_user`, `find_user`, …) with JSON-schema
  inputs derived from the DTOs.
- The MCP server holds **one** agent key; all scope enforcement happens
  server-side in auth-service (the MCP layer is not trusted to self-limit).
- Every call is audited with the resolving `AgentClient` identity (Phase E).
- Example tool surface:

```
create_user(email: string, sendInvite?: boolean) -> { userId, email }
revoke_session(sessionId: string) -> { revoked: true }
```

---

## 8. Phase E — Audit Trail (woven through A–D)

New entity **`AuditLog`**: `id`, `agentClientId`, `tool`/`action`, `targetType`,
`targetId`, `outcome` (success/denied/error), `metadata` (JSON), `createdAt`.

- Written via a single Spring interceptor/AOP aspect around `/agent/v1/**` so no
  endpoint can forget to log.
- Captures both **allowed** and **denied** attempts (denied attempts are the
  security signal).
- Append-only; never updated or deleted by application code.
- Example record:

```json
{ "agent": "support-agent", "action": "revoke_session",
  "targetType": "session", "targetId": "412", "outcome": "success",
  "createdAt": "2026-06-13T05:00:00Z" }
```

---

## 9. Recommended sequencing

Permissions and audit must exist **before** the mutating tools, so a misconfigured
agent can't run unchecked.

1. **A — Agent access**: `AgentClient` entity, hashing reuse, `AgentAuthenticationFilter`, admin bootstrap key. (+ Flyway migration)
2. **E — Audit foundation**: `AuditLog` + interceptor. Land early so every later endpoint is covered from day one.
3. **C — Permission model**: scope catalog + `@PreAuthorize` enforcement.
4. **B — Tools**: read-only first (`find_user`, `list_sessions`), then mutating (`disable_user` etc., including the `User.status` migration).
5. **D — MCP server**: wire tools to the now-secured endpoints.

---

## 10. Decisions needed before coding

1. **Credential format & presentation** — `Authorization: Bearer ak_…` vs `X-Agent-Key`. (Bearer is simpler for MCP HTTP clients; risk of confusion with user JWTs.)
2. **Agent identity model** — separate `AgentClient` entity (recommended) vs a `type` flag on `User`.
3. **API keys now vs OAuth2 client-credentials later** — start with keys, define the migration path.
4. **MCP server location** — same repo (`/mcp` module) vs separate repository.
5. **Scope granularity** — per-resource (`users:write`) vs per-action (`users:disable`). Start coarse, refine.
6. **Out of scope for now:** PostgreSQL/Flyway already exist; no infra work needed. No Docker/CI changes required for this layer.

---

## 11. What this is NOT (yet)

Per current direction: no implementation in this pass. This document is the
contract to review and amend before any `/agent/v1` code, `AgentClient` entity,
or MCP server is written.
