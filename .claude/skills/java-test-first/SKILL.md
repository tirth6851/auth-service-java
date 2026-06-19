---
name: java-test-first
description: Plan tests before implementing code, ensure high test coverage
tags: [testing, java, quality-assurance]
---

# Java Test-First Skill

Plan and implement tests before or alongside feature code. Ensures code is testable, maintainable, and validated.

## Requirements

Before starting:
1. Read `docs/TEST_STRATEGY.md` for test expectations
2. Understand test layers: unit (isolated), integration (full stack), security (auth/validation)
3. Check coverage goals: 80%+ overall, 85%+ for services, 90%+ for security code

## Process

### 1. Test Planning Phase
Identify what needs testing:
- Happy path: valid input → expected output
- Error paths: invalid input → appropriate error code (400, 401, 409)
- Security: password hashing, token validation, no secrets leaked
- Edge cases: empty strings, null values, boundary conditions

### 2. Unit Tests (Service Layer)
Test business logic in isolation using Mockito.

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository repository;
    
    @InjectMocks
    private AuthService service;
    
    @Test
    void signup_withValidEmail_createsUser() {
        // Given
        SignupRequest req = new SignupRequest("user@example.com", "Pass123");
        
        // When
        AuthResponse response = service.signup(req);
        
        // Then
        assertThat(response.getToken()).isNotBlank();
        verify(repository).save(any(User.class));
    }
    
    @Test
    void signup_withDuplicateEmail_throws409() {
        // Given
        SignupRequest req = new SignupRequest("user@example.com", "Pass123");
        when(repository.findByEmail("user@example.com"))
            .thenReturn(Optional.of(new User("user@example.com", "hash")));
        
        // When & Then
        assertThatThrownBy(() -> service.signup(req))
            .isInstanceOf(ResponseStatusException.class)
            .extracting("status")
            .isEqualTo(HttpStatus.CONFLICT);
    }
}
```

### 3. Integration Tests (Controller + Service + Repository)
Test full request/response cycle with real database (H2).

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTest {
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void signup_withValidInput_returns200AndToken() {
        // Given
        SignupRequest req = new SignupRequest("user@example.com", "Pass123");
        
        // When
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/auth/signup",
            req,
            AuthResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getToken()).isNotBlank();
    }
    
    @Test
    void signup_withDuplicateEmail_returns409() {
        // Given
        SignupRequest req = new SignupRequest("user@example.com", "Pass123");
        restTemplate.postForEntity("http://localhost:" + port + "/auth/signup", req, AuthResponse.class);
        
        // When
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/auth/signup",
            req,
            ErrorResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
```

### 4. Security Tests
Validate password hashing, token validation, error handling.

```java
@Test
void passwordIsNeverReturnedInResponse() {
    // Given
    SignupRequest req = new SignupRequest("user@example.com", "SecurePass123");
    
    // When
    ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
        "http://localhost:" + port + "/auth/signup",
        req,
        AuthResponse.class
    );
    
    // Then
    String json = objectMapper.writeValueAsString(response.getBody());
    assertThat(json).doesNotContain("password", "SecurePass123");
}

@Test
void passwordIsHashedWithBCrypt() {
    // Given
    User user = new User("user@example.com", BCrypt.hashpw("mypass", BCrypt.gensalt()));
    
    // When
    boolean matches = BCrypt.checkpw("mypass", user.getPasswordHash());
    
    // Then
    assertThat(matches).isTrue();
    assertThat(user.getPasswordHash()).isNotEqualTo("mypass");
}
```

### 5. Run Tests

```bash
# All tests
mvn test

# Single test class
mvn test -Dtest=AuthServiceTest

# With coverage report
mvn clean test jacoco:report
# Report: target/site/jacoco/index.html
```

## Coverage Goals

| Layer | Target |
|-------|--------|
| Service (AuthService) | 85%+ |
| Controller (AuthController) | 80%+ |
| Security (JwtUtil, Filter) | 90%+ |
| **Overall** | **80%+** |

## Test File Locations

- **Unit tests**: `src/test/java/com/authplatform/service/`
- **Integration tests**: `src/test/java/com/authplatform/controller/`
- **Security tests**: Add to integration or create `src/test/java/com/authplatform/security/`

## Checklist Before Code Review

- [ ] All unit tests written for service logic
- [ ] All integration tests written for controller endpoints
- [ ] Happy path tested (valid input → success)
- [ ] Error paths tested (invalid input → error code)
- [ ] Security tests verify passwords are hashed, tokens are valid
- [ ] No plaintext passwords or secrets in test data
- [ ] Test data is realistic and representative
- [ ] All tests pass: `mvn test`
- [ ] Coverage report shows ≥ 80% overall
- [ ] Test names are descriptive: `methodName_givenCondition_expectedResult`

## Anti-Patterns to Avoid

- ❌ Skipping tests (tests are required)
- ❌ Mocking service layer (mock repositories, not services)
- ❌ Testing implementation details instead of behavior
- ❌ Hardcoded test data (use fixtures or builders)
- ❌ Slow tests (unit tests should complete in <100ms)
- ❌ Interdependent tests (each test must be runnable independently)

## References

- [docs/TEST_STRATEGY.md](../../docs/TEST_STRATEGY.md) — test expectations
- [docs/ENGINEERING_STANDARDS.md](../../docs/ENGINEERING_STANDARDS.md) — testing rules
