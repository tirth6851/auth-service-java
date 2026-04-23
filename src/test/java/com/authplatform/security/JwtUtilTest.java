package com.authplatform.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

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
