# ADR 002: Controllers Are HTTP-Only, All Business Logic Lives in Service

**Date:** 2024-06-19  
**Status:** Accepted  
**Context:** Enforcing clean separation of concerns in layered architecture.

## Problem

Where should business logic live?
- **Controllers**: Near HTTP handling, convenient, but makes testing hard and mixes concerns
- **Services**: Separate from HTTP, testable, reusable, aligned with dependency injection

## Decision

**Controllers are HTTP boundaries only.** All validation, computation, and state changes live in service layer.

Controllers responsibilities:
- Parse DTO from HTTP request
- Validate DTO (Spring Bean Validation `@Valid`)
- Delegate to service
- Return DTO in response

Service responsibilities:
- All business logic (signup, login, password checks)
- Validation beyond DTO (business rules: duplicate email, invalid credentials)
- Database operations (via repository)
- Transaction management
- Exception throwing (service raises ResponseStatusException, not controller)

## Rationale

1. **Testability**: Service can be unit-tested without Spring context or HTTP mocking
2. **Reusability**: Service can be called from CLI, scheduled tasks, or other protocols
3. **Maintainability**: Business logic is centralized, easier to find and modify
4. **Single Responsibility**: Controller = HTTP transport, Service = business logic

## Consequences

- Controllers are thin (~10 lines per endpoint)
- Service layer is larger, contains all the logic
- Testing services is straightforward (no @SpringBootTest needed for unit tests)
- New developers must understand to put logic in service, not controller

## Alternatives Considered

1. **Logic in controller**: Simpler initially, but leads to untestable god-controllers
2. **Logic split between layers**: Ambiguous, hard to maintain

## Enforcement

- Code review: reject PRs with business logic in controller
- IDE inspection: set up checkstyle rule if needed

## Example

**❌ Wrong (logic in controller):**
```java
@PostMapping("/auth/signup")
public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest req) {
    if (userRepository.findByEmail(req.getEmail()).isPresent()) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email exists");
    }
    String hash = BCrypt.hashpw(req.getPassword(), BCrypt.gensalt());
    User user = new User(req.getEmail(), hash);
    userRepository.save(user);
    String token = jwtUtil.generateToken(user.getId(), user.getEmail());
    return ResponseEntity.ok(new AuthResponse(token, ...));
}
```

**✅ Right (logic in service):**
```java
@PostMapping("/auth/signup")
public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest req) {
    AuthResponse response = authService.signup(req);
    return ResponseEntity.ok(response);
}
```

```java
@Service
public class AuthService {
    public AuthResponse signup(SignupRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        String hash = BCrypt.hashpw(req.getPassword(), BCrypt.gensalt());
        User user = new User(req.getEmail(), hash);
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, ...);
    }
}
```
