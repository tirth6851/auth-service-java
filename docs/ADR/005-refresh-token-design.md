# ADR-005: Refresh Token Design

**Date**: 2026-06-19  
**Status**: Accepted

## Context

Phase 1 issues a single access token (JWT, 1-hour TTL) on login. This prevents explicit logout and makes the revocation window too wide for sensitive deployments. Refresh tokens let clients hold short-lived access tokens while maintaining sessions across access-token expiry.

## Decision

### Token Pair

| Token | Type | TTL | Where stored |
|-------|------|-----|--------------|
| Access token | JWT (HS256) | 1 h (configurable: `app.jwt.expiration-ms`) | Client memory only |
| Refresh token | Opaque UUID v4 | 7 d (configurable: `app.refresh-token.ttl-ms`) | DB + client |

Login and signup both return the token pair. `POST /auth/refresh` accepts a refresh token and returns a new pair. `POST /auth/logout` revokes the refresh token.

### Storage: SHA-256 hash, not plaintext

The DB stores `SHA-256(rawToken)` as a 64-char hex string. The client receives the raw UUID.

- **Why not plaintext?** A compromised DB gives an attacker working tokens.  
- **Why SHA-256, not bcrypt?** UUID v4 has 122-bit entropy — no salt needed. bcrypt is intentionally slow and adds latency without benefit for random high-entropy values.  
- **Implementation**: `HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(...))` — Java 17 standard library, no added dependencies.

### Rotation on every refresh

`POST /auth/refresh`:
1. Find the hash of the submitted token in DB
2. Check not expired, not revoked → throw 401 otherwise
3. Set `revokedAt = now()` on the old token
4. Issue new access token + new refresh token
5. Return both

Rotation limits the exploit window if a refresh token is stolen.

### Reuse detection (implemented)

When a refresh token that is **already revoked** (rotated away or logged out) is replayed, that is
treated as a theft signal. `RefreshTokenService.validateAndRotate` calls
`RefreshTokenReuseHandler.revokeFamily(userId)`, which revokes **every still-active refresh token**
for that user, then rejects the request with 401. Both the attacker's copy and the legitimate
client are forced to re-authenticate.

The family revocation runs in a separate `@Transactional(REQUIRES_NEW)` transaction (in a dedicated
bean, so the Spring proxy applies) so it is **committed even though the triggering request aborts
with 401 and its own transaction rolls back**. This is verified end-to-end by
`AuthControllerIntegrationTest.refresh_reuseOfRotatedToken_revokesEntireTokenFamily`.

Trade-off: an attacker holding only an *old, already-rotated* token can force-revoke the victim's
session (a self-limiting nuisance, not credential theft). This is the standard, accepted posture for
rotating refresh tokens — availability is sacrificed to guarantee a stolen token cannot be used.

**Known UX sharp edge (accepted):** a legitimate client that submits `/auth/refresh`, has the
request succeed server-side but *not* see the response (network timeout), and then **retries with
the same token** will trip reuse detection and be logged out of **every** device. Note this is only
the *sequential* retry-after-commit case: two *concurrent* refreshes of the same token do not both
enter the reuse branch — the optimistic-lock loser fails at commit (→401) without revoking the
family (see "Concurrency guard"). A future mitigation, if the mass-logout proves too aggressive, is a
short **grace window** that accepts the immediately-previous token for a few seconds instead of
treating it as reuse. Deferred; current posture favors safety.

**Operational note:** `revokeFamily`'s `REQUIRES_NEW` briefly holds two connections from the pool
(the suspended outer transaction plus the new one). Fine at HikariCP's default pool size (10); would
self-deadlock only at a pathologically small pool (size 1).

### Revocation model

`revokedAt` is a nullable timestamp. Revoked = `revokedAt IS NOT NULL`. Tokens are never deleted (supports audit trail). `POST /auth/logout` sets `revokedAt`; idempotent on already-revoked tokens (returns 204).

### Concurrency guard (implemented)

Concurrent refresh with the same token could previously both succeed under READ_COMMITTED. This is
now guarded by `@Version` optimistic locking on `RefreshToken` (column added in
`V3__add_version_to_refresh_tokens.sql`). Two concurrent rotations of the same token cannot both
commit — the loser fails with an optimistic-lock exception, mapped to 401 by `GlobalExceptionHandler`
(same non-revealing shape as other invalid-token responses).

## Database

`V2__create_refresh_tokens_table.sql` adds the `refresh_tokens` table in PostgreSQL. Dev/test continues to use H2 + `ddl-auto=update` (Flyway disabled). The `created_at` column has no DB default — set by `@PrePersist`, matching V1 `users.created_at`.

## Access token TTL note

Access tokens default to 1 hour (`application.properties`, used by dev/H2). **Production is now set
to 15 minutes** (`app.jwt.expiration-ms=900000` in `application-prod.properties`) because a stateless
JWT cannot be revoked before expiry; the short TTL bounds the window and the DB-backed refresh token
carries the session. Tests are not sensitive to this value.

## Consequences

- `POST /auth/login` and `POST /auth/signup` responses now include `refreshToken`
- `AuthResponse` gets a `refreshToken` field annotated `@JsonInclude(NON_NULL)` — backward-compatible
- New endpoints: `POST /auth/refresh`, `POST /auth/logout`
- New entity: `RefreshToken`, `RefreshTokenRepository`, `RefreshTokenService`
