# Phase 1 Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add test coverage for the complete Phase 1 implementation (JwtUtil, AuthService, AuthController HTTP contracts).

**Architecture:** Three test files — pure unit test for JwtUtil, mocked-dependencies unit test for AuthService, and a full @SpringBootTest MockMvc integration test for AuthController.

**Tech Stack:** JUnit 5, Mockito, Spring Boot Test, Spring Security Test, H2 (in-memory, already configured).

---

### Task 1: JwtUtil unit tests

**Files:**
- Create: `src/test/java/com/authplatform/security/JwtUtilTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.authplatform.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "TestSecretKeyOfAtLeastThirtyTwoChars!!");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L);
        jwtUtil.init();
    }

    @Test
    void generateToken_containsUserIdAndEmail() {
        String token = jwtUtil.generateToken(1L, "a@b.com");
        Claims claims = jwtUtil.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("email", String.class)).isEqualTo("a@b.com");
    }

    @Test
    void isTokenValid_returnsTrueForValidToken() {
        String token = jwtUtil.generateToken(1L, "a@b.com");
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForGarbage() {
        assertThat(jwtUtil.isTokenValid("not.a.token")).isFalse();
    }

    @Test
    void extractUserId_returnsCorrectId() {
        String token = jwtUtil.generateToken(42L, "a@b.com");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String token = jwtUtil.generateToken(1L, "x@y.com");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("x@y.com");
    }
}
```

- [ ] **Step 2: Run to verify failure**

```bash
mvn test -Dtest=JwtUtilTest -pl . 2>&1 | tail -20
```
Expected: compilation error (class doesn't exist yet) or test runner cannot find class.

- [ ] **Step 3: Run tests to verify they pass**

The implementation already exists. Run:
```bash
mvn test -Dtest=JwtUtilTest
```
Expected: 5 tests pass.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/authplatform/security/JwtUtilTest.java
git commit -m "test: add JwtUtil unit tests"
```

---

### Task 2: AuthService unit tests

**Files:**
- Create: `src/test/java/com/authplatform/service/AuthServiceTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.authplatform.service;

import com.authplatform.dto.AuthResponse;
import com.authplatform.dto.LoginRequest;
import com.authplatform.dto.SignupRequest;
import com.authplatform.model.User;
import com.authplatform.repository.UserRepository;
import com.authplatform.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void signup_returnsToken_whenEmailIsNew() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("hashed");
        User saved = new User("a@b.com", "hashed");
        saved.setId(1L);
        when(userRepository.save(any())).thenReturn(saved);
        when(jwtUtil.generateToken(1L, "a@b.com")).thenReturn("tok");

        SignupRequest req = new SignupRequest();
        req.setEmail("  A@B.COM  ");
        req.setPassword("pass1234");

        AuthResponse resp = authService.signup(req);
        assertThat(resp.getToken()).isEqualTo("tok");
    }

    @Test
    void signup_throws409_whenEmailTaken() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        SignupRequest req = new SignupRequest();
        req.setEmail("a@b.com");
        req.setPassword("pass1234");

        assertThatThrownBy(() -> authService.signup(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    @Test
    void login_returnsToken_withValidCredentials() {
        User user = new User("a@b.com", "hashed");
        user.setId(1L);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass1234", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "a@b.com")).thenReturn("tok");

        LoginRequest req = new LoginRequest();
        req.setEmail("a@b.com");
        req.setPassword("pass1234");

        AuthResponse resp = authService.login(req);
        assertThat(resp.getToken()).isEqualTo("tok");
    }

    @Test
    void login_throws401_withWrongPassword() {
        User user = new User("a@b.com", "hashed");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setEmail("a@b.com");
        req.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }

    @Test
    void login_throws401_withUnknownEmail() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setEmail("x@y.com");
        req.setPassword("pass1234");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }
}
```

- [ ] **Step 2: Run to verify they pass**

```bash
mvn test -Dtest=AuthServiceTest
```
Expected: 5 tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/authplatform/service/AuthServiceTest.java
git commit -m "test: add AuthService unit tests"
```

---

### Task 3: AuthController integration tests

**Files:**
- Create: `src/test/java/com/authplatform/controller/AuthControllerIntegrationTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.authplatform.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthControllerIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void signup_returns200AndToken() throws Exception {
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"alice@example.com","password":"pass1234"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void signup_returns409_onDuplicateEmail() throws Exception {
        String body = """
            {"email":"alice@example.com","password":"pass1234"}
            """;
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content(body));
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void signup_returns400_onInvalidEmail() throws Exception {
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"notanemail","password":"pass1234"}
                    """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signup_returns400_onShortPassword() throws Exception {
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"a@b.com","password":"short"}
                    """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returns200AndToken() throws Exception {
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"bob@example.com","password":"pass1234"}
                    """));
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"bob@example.com","password":"pass1234"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_returns401_onWrongPassword() throws Exception {
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"bob@example.com","password":"pass1234"}
                    """));
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"bob@example.com","password":"wrongpass"}
                    """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returns401_onUnknownEmail() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"nobody@example.com","password":"pass1234"}
                    """))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: Run to verify they pass**

```bash
mvn test -Dtest=AuthControllerIntegrationTest
```
Expected: 7 tests pass.

- [ ] **Step 3: Run full suite**

```bash
mvn test
```
Expected: all 17 tests pass (5 + 5 + 7).

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/authplatform/controller/AuthControllerIntegrationTest.java
git commit -m "test: add AuthController integration tests — all Phase 1 acceptance tests covered"
```

---

## Completion Audit

After all three tasks, verify `docs/done-criteria.md` item by item:

```bash
mvn compile          # must exit 0
mvn spring-boot:run  # must start on :8080 (Ctrl+C to stop)
mvn test             # must show BUILD SUCCESS
```
