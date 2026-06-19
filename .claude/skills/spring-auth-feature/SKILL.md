---
name: spring-auth-feature
description: Implement backend auth features in layered Spring Boot style
tags: [backend, spring-boot, java, auth]
---

# Spring Auth Feature Skill

Implement new authentication and user management features following the auth-service-java layered architecture.

## Requirements

Before starting:
1. Read `CLAUDE_SESSION_START.md` for session context
2. Review `docs/ENGINEERING_STANDARDS.md` for coding rules
3. Understand current architecture: `docs/ARCHITECTURE.md`
4. Check API contract: `docs/API_CONTRACT.md`

## Process

### 1. Plan Phase
- Identify which layers are affected: controller, service, repository, model, DTO, security
- Update relevant docs (API_CONTRACT.md, ARCHITECTURE.md) if behavior changes
- Check roadmap alignment: `PROJECT_BACKLOG`

### 2. Code Phase
- **Controllers**: HTTP-only; parse DTO, call service, return response
- **Services**: All business logic; validation, transactions, exception handling
- **Repositories**: Spring Data JPA interfaces only; no custom queries
- **Models**: JPA entities; clean mapping to DTOs
- **DTOs**: Request/response objects with Bean Validation annotations; never expose entities
- **Tests**: Unit tests for service, integration tests for controller

### 3. Quality Phase
- Code follows [ENGINEERING_STANDARDS.md](../../docs/ENGINEERING_STANDARDS.md)
- Unit test coverage ≥ 80% for service layer
- Integration tests cover happy path + error paths
- All tests pass: `mvn test`
- No plaintext passwords or secrets in code
- Run `/security-review` skill if touching auth or config

### 4. Documentation Phase
- Update relevant docs in `docs/` if behavior changed
- If architecture changes: create new `docs/ADR/` decision record
- Update README if user-facing behavior changed

## Common Patterns

### Adding a New Endpoint

**1. Create DTO:**
```java
// src/main/java/com/authplatform/dto/MyRequest.java
public class MyRequest {
    @NotNull @Email
    private String email;
    // Validation annotations, getters, setters
}
```

**2. Add to Service:**
```java
// src/main/java/com/authplatform/service/AuthService.java
public MyResponse myFeature(MyRequest req) {
    // Validation, business logic
    return new MyResponse(...);
}
```

**3. Add to Controller:**
```java
@PostMapping("/auth/my-feature")
public ResponseEntity<MyResponse> myFeature(@Valid @RequestBody MyRequest req) {
    MyResponse response = authService.myFeature(req);
    return ResponseEntity.ok(response);
}
```

**4. Test:**
```java
// Integration test in AuthControllerIntegrationTest
@Test
void myFeature_withValidInput_returns200() { ... }

@Test
void myFeature_withInvalidInput_returns400() { ... }
```

### Extending User Model

**1. Add field to entity:**
```java
@Entity
public class User {
    // ... existing fields
    private String newField;
}
```

**2. Create migration (Flyway, Phase 2+):**
```sql
-- src/main/resources/db/migration/V2__add_new_field.sql
ALTER TABLE user ADD COLUMN new_field VARCHAR(255);
```

**3. Update DTO if needed:**
```java
public class UserResponse {
    private String newField;  // Only if user-facing
}
```

**4. Update service to populate new field:**
```java
user.setNewField(value);
```

## Checklist Before Marking Done

- [ ] Feature is implemented and works locally
- [ ] Code follows layered architecture (logic in service, not controller)
- [ ] DTOs used for all HTTP requests/responses
- [ ] Unit tests pass: `mvn test` (80%+ coverage for service)
- [ ] Integration tests cover happy path + error cases
- [ ] No plaintext passwords, secrets, or sensitive data exposed
- [ ] Error messages are user-friendly, don't leak internals
- [ ] Relevant docs updated (API_CONTRACT.md, ARCHITECTURE.md)
- [ ] New decision recorded in docs/ADR/ if architecture changed

## Anti-Patterns to Avoid

- ❌ Logic in controller (move to service)
- ❌ Exposing JPA entities in responses (use DTOs)
- ❌ Direct repository calls in controller (go through service)
- ❌ Passwords in logs or responses (hash with BCrypt, never expose)
- ❌ Skipping tests (tests required for every feature)
- ❌ Modifying application.properties without checking production impact

## References

- [CLAUDE.md](../../CLAUDE.md) — project overview
- [docs/ARCHITECTURE.md](../../docs/ARCHITECTURE.md) — system design
- [docs/ENGINEERING_STANDARDS.md](../../docs/ENGINEERING_STANDARDS.md) — coding rules
- [docs/API_CONTRACT.md](../../docs/API_CONTRACT.md) — endpoint specs
