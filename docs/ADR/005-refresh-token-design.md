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

Rotation limits the exploit window if a refresh token is stolen: the next legitimate refresh attempt detects the collision and revokes the session.

### Revocation model

`revokedAt` is a nullable timestamp. Revoked = `revokedAt IS NOT NULL`. Tokens are never deleted (supports audit trail). `POST /auth/logout` sets `revokedAt`; idempotent on already-revoked tokens (returns 204).

### Known limitation

Concurrent refresh with the same token under READ_COMMITTED isolation can both succeed. Mitigation: `@Version` optimistic locking on `RefreshToken` can be added as a future enhancement. Risk is negligible at current scale.

## Database

`V2__create_refresh_tokens_table.sql` adds the `refresh_tokens` table in PostgreSQL. Dev/test continues to use H2 + `ddl-auto=update` (Flyway disabled). The `created_at` column has no DB default — set by `@PrePersist`, matching V1 `users.created_at`.

## Access token TTL note

Access tokens remain at 1 hour by default. With refresh tokens deployed, production environments should reduce this to 15 minutes (`app.jwt.expiration-ms=900000`). Tests are not sensitive to this value.

## Consequences

- `POST /auth/login` and `POST /auth/signup` responses now include `refreshToken`
- `AuthResponse` gets a `refreshToken` field annotated `@JsonInclude(NON_NULL)` — backward-compatible
- New endpoints: `POST /auth/refresh`, `POST /auth/logout`
- New entity: `RefreshToken`, `RefreshTokenRepository`, `RefreshTokenService`
