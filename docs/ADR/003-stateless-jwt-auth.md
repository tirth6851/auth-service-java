# ADR 003: Stateless JWT Authentication (No Session Storage)

**Date:** 2024-06-19  
**Status:** Accepted  
**Context:** Choosing authentication model for a distributed, scalable service.

## Problem

How should we store and validate authentication state?
- **Sessions** (database/cache): Server stores session, client holds session ID cookie
- **Stateless JWT**: Client holds token (JWT), server validates signature only

## Decision

**Stateless JWT authentication.** No session table, no server-side token storage.

Clients receive JWT on login and include it in `Authorization: Bearer <token>` header for all requests. Server validates the JWT signature; if valid and not expired, the request is authenticated.

## Rationale

1. **Horizontal scaling**: Any server instance can validate any token (no shared session store needed)
2. **Simplicity**: No session management code, no cache invalidation
3. **Mobile/SPA friendly**: Tokens work naturally with HTTP headers (no cookies needed)
4. **Logout complexity**: Skipped for Phase 1 (tokens valid until expiry; revocation not needed yet)
5. **Audit trail**: Token claims are self-contained and immutable

## Consequences

- No explicit logout; tokens valid until expiry
- No server-side token revocation (planned for Phase 2)
- Token size is slightly larger than session ID
- Client responsible for storing token securely (memory or secure storage, not localStorage)

## Alternatives Considered

1. **Database sessions**: Requires session table + lookup on every request; doesn't scale horizontally without shared DB
2. **Redis sessions**: Requires external cache; adds operational complexity
3. **Hybrid**: JWT for API, sessions for web UI; over-engineered for Phase 1

## Security Implications

- **Token theft**: If attacker steals token, they can use it until expiry (HTTPS required to prevent theft)
- **No logout**: Users can't instantly revoke access; tokens remain valid until TTL (acceptable for Phase 1)
- **Signature validation**: Server must never trust unsigned tokens; JJWT library handles this

## Implementation

```java
// On login, issue JWT
String token = Jwts.builder()
    .subject(userId.toString())
    .claim("email", userEmail)
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + 3600000))
    .signWith(key)
    .compact();
return new AuthResponse(token, expiryTime);

// On protected request, validate JWT
String token = extractTokenFromHeader(request);
Claims claims = Jwts.parserBuilder()
    .verifyWith(key)
    .build()
    .parseSignedClaims(token)
    .getPayload();
// If parsing succeeds, token is valid; if invalid/expired, exception thrown and caught by filter
```

## Future Considerations (Phase 2+)

- **Token revocation**: Add blacklist/whitelist table for immediate logout
- **Refresh tokens**: Separate short-lived access tokens + long-lived refresh tokens
- **Token binding**: Tie tokens to IP/device to prevent theft
