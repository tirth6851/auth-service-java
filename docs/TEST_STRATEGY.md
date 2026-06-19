# Test Strategy

## Overview

Auth Platform uses a three-tier testing approach: unit tests (business logic), integration tests (full stack), and security tests (auth/validation).

**Goal:** 80%+ code coverage, all tests passing, zero known security issues.

---

## Test Layers

### Unit Tests (Isolated)

**Purpose:** Validate business logic in isolation, fast feedback, high coverage.

**Scope:** Service and utility classes (AuthService, JwtUtil).

**Mock:** Database, external dependencies.

**Tools:** JUnit 5, Mockito, AssertJ.

**Example:**
```java
@Test
void signup_hashedPasswordDoesNotEqual_plainPassword() {
    // Given
    String plainPassword = "MyPassword123";
    User user = new User("user@example.com", plainPassword);
    
    // When
    String hashed = user.getPasswordHash();
    
    // Then
    assertThat(hashed).isNotEqualTo(plainPassword);
    assertThat(BCrypt.checkpw(plainPassword, hashed)).isTrue();
}
```

**Files:**
- `src/test/java/com/authplatform/service/AuthServiceTest.java`
- `src/test/java/com/authplatform/security/JwtUtilTest.java`

**Run:**
```bash
mvn test -Dtest=AuthServiceTest
```

---

### Integration Tests (Full Stack)

**Purpose:** Validate request → response cycle, real database, realistic scenarios.

**Scope:** Controller + service + repository + database.

**Setup:** `@SpringBootTest` with embedded H2, no mocks.

**Tools:** Spring Test, TestRestTemplate, H2.

**Example:**
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTest {
    @LocalServerPort
    private int port;
    
    @Test
    void signup_withValidInput_returns200AndToken() {
        // Given
        SignupRequest request = new SignupRequest("user@example.com", "Pass123");
        
        // When
        ResponseEntity<AuthResponse> response = testRestTemplate.postForEntity(
            "http://localhost:" + port + "/auth/signup",
            request,
            AuthResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getToken()).isNotBlank();
    }
    
    @Test
    void signup_withDuplicateEmail_returns409() {
        // Given
        SignupRequest request1 = new SignupRequest("user@example.com", "Pass123");
        SignupRequest request2 = new SignupRequest("user@example.com", "Pass456");
        
        // When
        testRestTemplate.postForEntity("http://localhost:" + port + "/auth/signup", request1, AuthResponse.class);
        ResponseEntity<ErrorResponse> response2 = testRestTemplate.postForEntity(
            "http://localhost:" + port + "/auth/signup",
            request2,
            ErrorResponse.class
        );
        
        // Then
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response2.getBody().getMessage()).contains("already registered");
    }
}
```

**Files:**
- `src/test/java/com/authplatform/controller/AuthControllerIntegrationTest.java`

**Run:**
```bash
mvn test -Dtest=AuthControllerIntegrationTest
```

---

### Security Tests (Auth & Validation)

**Purpose:** Validate authentication, authorization, error handling, no information leakage.

**Scope:** JWT validation, password hashing, error responses, sensitive data.

**Testing checklist:**
- [ ] Passwords are BCrypt-hashed, never plaintext
- [ ] Invalid/expired tokens return 401
- [ ] Missing auth header returns 401
- [ ] Duplicate email returns 409 (not 500)
- [ ] Invalid email format returns 400 (not 500)
- [ ] Error responses don't leak stack traces or internals
- [ ] Password is never returned in any response
- [ ] Token is valid JWT and decodable

**Example:**
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SecurityIntegrationTest {
    
    @Test
    void login_withWrongPassword_returns401() {
        // Given
        SignupRequest signup = new SignupRequest("user@example.com", "CorrectPass123");
        testRestTemplate.postForEntity("http://localhost:" + port + "/auth/signup", signup, AuthResponse.class);
        
        LoginRequest login = new LoginRequest("user@example.com", "WrongPassword");
        
        // When
        ResponseEntity<ErrorResponse> response = testRestTemplate.postForEntity(
            "http://localhost:" + port + "/auth/login",
            login,
            ErrorResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getMessage()).doesNotContain("password"); // Don't leak info
    }
    
    @Test
    void passwordIsNeverReturnedInResponse() {
        // Given
        SignupRequest request = new SignupRequest("user@example.com", "SecurePass123");
        
        // When
        ResponseEntity<AuthResponse> response = testRestTemplate.postForEntity(
            "http://localhost:" + port + "/auth/signup",
            request,
            AuthResponse.class
        );
        
        // Then
        String responseJson = objectMapper.writeValueAsString(response.getBody());
        assertThat(responseJson).doesNotContain("password");
        assertThat(responseJson).doesNotContain("SecurePass123");
    }
}
```

---

## Coverage Goals

| Layer | Target | Current | Status |
|-------|--------|---------|--------|
| Service (AuthService) | 85%+ | TBD | Track in CI |
| Controller (AuthController) | 80%+ | TBD | Track in CI |
| Security (JwtUtil, Filter) | 90%+ | TBD | Track in CI |
| Repository | 60%+ | TBD | Lower priority (mostly boilerplate) |
| **Overall** | **80%+** | TBD | Track in CI |

Generate report:
```bash
mvn clean test jacoco:report
# Report: target/site/jacoco/index.html
```

---

## Test Execution Pipeline

**Local (Developer):**
```bash
mvn test  # All tests, ~30 sec
```

**CI/CD (Automated):**
1. Compile: `mvn clean compile`
2. Unit tests: `mvn test`
3. Integration tests: (same command, tagged `@SpringBootTest`)
4. Coverage report: `mvn jacoco:report`
5. Build JAR: `mvn package`

---

## Test Data Management

**H2 in-memory:** Fresh database created for each test suite (`spring.jpa.hibernate.ddl-auto=create-drop`).

**Fixtures:** Create test data in `@BeforeEach` or within test method.

**Example:**
```java
@BeforeEach
void setup() {
    User testUser = new User();
    testUser.setEmail("existing@example.com");
    testUser.setPasswordHash(BCrypt.hashpw("Password123", BCrypt.gensalt()));
    userRepository.save(testUser);
}

@Test
void login_withExistingUser_returnsToken() {
    LoginRequest request = new LoginRequest("existing@example.com", "Password123");
    // Test uses testUser from setup()
}
```

---

## Test Categories

### Must-Pass (Blocking)
- All unit tests in AuthServiceTest
- All integration tests in AuthControllerIntegrationTest
- Security tests (password, token, error handling)

### Should-Pass (Quality Gates)
- 80%+ overall code coverage
- No new security issues introduced

### Nice-to-Have (Future)
- Performance tests (BCrypt hash time)
- Load tests (concurrent signups/logins)
- Chaos tests (DB unavailable, slow queries)

---

## Failing Tests Protocol

If a test fails:

1. **Local reproduction:** Run test locally with `mvn test -Dtest=TestName`
2. **Debug:** Add breakpoint, check logs, inspect test data
3. **Common causes:**
   - Port already in use → kill process on 8080
   - H2 lock → clear target/ directory
   - Stale test data → ensure `create-drop` is set
   - Timing issue → add small sleep or use `await()` from awaitility library

4. **Fix:** Update test or code, re-run to confirm pass

---

## Continuous Integration (CI)

Example GitHub Actions workflow:

```yaml
name: Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: 17
      - run: mvn clean test
      - run: mvn jacoco:report
      - uses: codecov/codecov-action@v3
        with:
          files: ./target/site/jacoco/jacoco.xml
```

Push to branch → tests run → PR requires passing tests before merge.

---

## Test Maintenance

**Review quarterly:**
- Remove obsolete tests
- Update tests when API changes
- Add tests for new bugs discovered
- Refactor duplicated test code

**Keep tests:**
- Fast (< 5 sec total ideally)
- Isolated (no cross-test dependencies)
- Clear (descriptive names, easy to understand)
- Focused (one assertion per test, or related assertions)

---

## Appendix: Test Templates

### Unit Test Template
```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    @Mock
    private MyRepository repository;
    
    @InjectMocks
    private MyService service;
    
    @Test
    void methodName_withGivenInput_expectedOutput() {
        // Given
        
        // When
        
        // Then
    }
}
```

### Integration Test Template
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MyControllerTest {
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void endpointName_withGivenInput_expectedResponse() {
        // Given
        MyRequest request = new MyRequest(...);
        
        // When
        ResponseEntity<MyResponse> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/endpoint",
            request,
            MyResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```
