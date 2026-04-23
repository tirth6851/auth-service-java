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
