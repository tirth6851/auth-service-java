# ADR 001: JWT Subject Claim Contains User ID, Not Email

**Date:** 2024-06-19  
**Status:** Accepted  
**Context:** Designing JWT token structure for stateless authentication.  

## Problem

When validating tokens in protected endpoints, we need to identify which user made the request. Two options:
1. JWT subject = user ID (numeric, immutable)
2. JWT subject = email (user-mutable, could change)

## Decision

**JWT subject contains the user ID (Long as string).**

Also include email as a custom claim for convenience, but don't rely on it for authorization.

## Rationale

- **Immutability**: User ID never changes; email might. Token remains valid if user updates email.
- **Database queries**: Lookup by user ID is faster (primary key index) than email lookup.
- **Security**: Prevents email-change attacks where old email is transferred to another user.
- **Standards**: JWT RFC 7519 recommends subject as principal identifier; email is principal name, not identifier.

## Consequences

- Service layer must always map `sub` claim to user ID, then fetch user by ID
- Custom claim `email` is available for convenience but not authoritative (re-fetch from DB if needed)
- User profile updates (email change) don't invalidate existing tokens
- Token grants access based on user ID, not email

## Alternatives Considered

1. **Email as subject**: Simpler JWT decode, but breaks if email changes or is reassigned
2. **Both ID and email in JWT**: Redundant, increases token size

## Implementation

```java
// JWT creation
Jwts.builder()
    .subject(userId.toString())  // numeric user ID
    .claim("email", userEmail)    // custom claim
    .signWith(key)
    .compact();

// JWT validation & usage
Claims claims = Jwts.parserBuilder()
    .verifyWith(key)
    .build()
    .parseSignedClaims(token)
    .getPayload();

Long userId = Long.valueOf(claims.getSubject());  // Extract user ID
User user = userRepository.findById(userId);      // Fetch by ID
```
