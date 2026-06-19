# ADR 004: HTTP Requests/Responses Use DTOs, Never Expose Entities

**Date:** 2024-06-19  
**Status:** Accepted  
**Context:** Decoupling API contract from internal database schema.

## Problem

Should we return JPA entities directly in REST responses?
- **Entity exposure**: Simple, less code, but couples API to DB schema
- **DTOs**: Extra mapping code, but API is independent of schema changes

## Decision

**All HTTP requests and responses use DTOs.** JPA entities are never serialized or returned to clients.

DTOs live in `com.authplatform.dto` package. Entities live in `com.authplatform.model` package.

## Rationale

1. **API stability**: API contract doesn't change when DB schema changes
2. **Security**: Never accidentally expose internal fields (e.g., password_hash, audit columns)
3. **Validation**: DTOs have Bean Validation annotations (`@NotNull`, `@Email`), entities don't
4. **Evolution**: Can add fields to entity without breaking API compatibility

## Consequences

- More code: service must map between entities and DTOs
- Overhead: extra object allocation (minimal in practice)
- Testing: DTOs must be tested alongside entities

## Alternatives Considered

1. **Entity inheritance**: Entities extend DTOs; couples API to DB (rejected)
2. **Entity annotations**: Add `@JsonIgnore` to sensitive fields; works but fragile
3. **Projections**: Spring Data JPA projections; overkill for Phase 1

## Implementation

```java
// Entity (database representation)
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String passwordHash;  // Never exposed
    
    private Instant createdAt;
    private Instant updatedAt;
    // Getters/setters...
}

// DTOs (HTTP request/response)
public class SignupRequest {
    @NotNull
    @Email
    private String email;
    
    @NotNull
    @Size(min = 6)
    private String password;  // Plaintext, hashed in service
}

public class AuthResponse {
    private String token;
    private Long expiresAt;
    private String email;  // For convenience, NOT from entity.password
    // No password field
}

// Service maps entity to DTO
public AuthResponse signup(SignupRequest req) {
    User user = new User(req.getEmail(), hashedPassword);
    userRepository.save(user);
    String token = jwtUtil.generateToken(user.getId(), user.getEmail());
    return new AuthResponse(token, expirationTime, user.getEmail());  // DTO, not entity
}
```

## Enforcement

- Code review: reject PRs that return entities in `@RestController` methods
- IDE inspection: mark `@Entity` classes as non-serializable if needed

## Benefits

**Schema change example:**
```java
// DB schema: add password_reset_token column
@Entity
public class User {
    // ... existing fields
    private String passwordResetToken;
}

// API unchanged: DTOs don't include this field
// AuthResponse still only has: token, expiresAt, email
```

If we had exposed the entity, all API clients would see the new field and might break.
