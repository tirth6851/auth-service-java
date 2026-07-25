package com.authplatform.controller;

import com.authplatform.repository.RefreshTokenRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired RefreshTokenRepository refreshTokenRepository;

    // --- Signup / Login ---

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
            {"email":"dup@example.com","password":"pass1234"}
            """;
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
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
                    """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"bob@example.com","password":"pass1234"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_returns401_onWrongPassword() throws Exception {
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"wrongpw@example.com","password":"pass1234"}
                    """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"wrongpw@example.com","password":"wrongpass"}
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

    // --- Protected routes ---

    @Test
    void protectedRoute_returns401_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_returns401_whenInvalidToken() throws Exception {
        mockMvc.perform(get("/api/protected")
                .header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_passesAuth_whenValidToken() throws Exception {
        String signupResponse = mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"tokenuser@example.com","password":"pass1234"}
                    """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(signupResponse, "$.token");

        // A valid token passes authentication; /api/protected doesn't exist so Spring returns 404,
        // confirming the request was not rejected by the security layer.
        mockMvc.perform(get("/api/protected")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // --- GET /auth/me tests ---

    @Test
    void authMe_returns200WithUserData_whenValidToken() throws Exception {
        String signupResponse = mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"meuser@example.com","password":"pass1234"}
                            """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(signupResponse, "$.token");

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("meuser@example.com"))
                .andExpect(jsonPath("$.verified").value(false))
                .andExpect(jsonPath("$.createdAt").isString());
    }

    @Test
    void authMe_returns401_whenNoToken() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authMe_returns401_whenInvalidToken() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer not.a.valid.token"))
                .andExpect(status().isUnauthorized());
    }

    // --- Rate limiting tests (/auth/login, capacity=3 in test config) ---

    @Test
    void login_underRateLimit_returns401NotRateLimited() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"underrate@example.com","password":"pass1234"}
                            """))
                .andExpect(status().isOk());

        // 2 of 3 allowed attempts — must get 401 (wrong creds), not 429
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {"email":"underrate@example.com","password":"wrongpass"}
                                """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Invalid credentials"));
        }
    }

    @Test
    void login_returns429_whenRateLimitExceeded() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"ratelimit@example.com","password":"pass1234"}
                            """))
                .andExpect(status().isOk());

        // Exhaust the bucket (capacity=3)
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {"email":"ratelimit@example.com","password":"wrongpass"}
                                """))
                    .andExpect(status().isUnauthorized());
        }

        // 4th attempt must be rate-limited
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"ratelimit@example.com","password":"wrongpass"}
                            """))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void login_returns429_withRetryAfterHeaderAndErrorBody() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"retryafter@example.com","password":"pass1234"}
                            """))
                .andExpect(status().isOk());

        // Exhaust the bucket (capacity=3)
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {"email":"retryafter@example.com","password":"wrongpass"}
                                """))
                    .andExpect(status().isUnauthorized());
        }

        // 4th attempt: 429 + Retry-After header + error body
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"retryafter@example.com","password":"wrongpass"}
                            """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Too many login attempts. Please try again later."));
    }

    // --- Rate limiting tests (/auth/signup, capacity=3 in test config, separate bucket from login) ---

    @Test
    void signup_underRateLimit_returns200NotRateLimited() throws Exception {
        // 2 of 3 allowed attempts — must succeed, not 429
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"signupunderrate1@example.com","password":"pass1234"}
                            """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"signupunderrate2@example.com","password":"pass1234"}
                            """))
                .andExpect(status().isOk());
    }

    @Test
    void signup_returns429_whenRateLimitExceeded() throws Exception {
        // Exhaust the bucket (capacity=3)
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {"email":"signupratelimit%d@example.com","password":"pass1234"}
                                """.formatted(i)))
                    .andExpect(status().isOk());
        }

        // 4th attempt must be rate-limited
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"signupratelimit4@example.com","password":"pass1234"}
                            """))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void signup_returns429_withRetryAfterHeaderAndErrorBody() throws Exception {
        // Exhaust the bucket (capacity=3)
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {"email":"signupretryafter%d@example.com","password":"pass1234"}
                                """.formatted(i)))
                    .andExpect(status().isOk());
        }

        // 4th attempt: 429 + Retry-After header + error body
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"signupretryafter4@example.com","password":"pass1234"}
                            """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Too many signup attempts. Please try again later."));
    }

    @Test
    void signupRateLimit_doesNotBlockLogin_andViceVersa() throws Exception {
        // Create a user first (uses one of the login-bucket-unrelated signup attempts)
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"separatebuckets@example.com","password":"pass1234"}
                            """))
                .andExpect(status().isOk());

        // Exhaust the login bucket (capacity=3) — this must not affect signup's bucket
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {"email":"separatebuckets@example.com","password":"wrongpass"}
                                """))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"separatebuckets@example.com","password":"wrongpass"}
                            """))
                .andExpect(status().isTooManyRequests());

        // Signup should still work (its bucket is independent) — only 1 of 3 signup
        // attempts consumed above, so this succeeds without hitting signup's limit.
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"separatebuckets2@example.com","password":"pass1234"}
                            """))
                .andExpect(status().isOk());
    }

    // --- Security header tests ---

    @Test
    void responses_includeXFrameOptionsSameOrigin() throws Exception {
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"frametest@example.com","password":"pass1234"}
                    """))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }

    // --- Error response shape ---

    @Test
    void errorShape_validationFailure_hasDetailsArray() throws Exception {
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"notanemail","password":"pass1234"}
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    @Test
    void errorShape_invalidCredentials_hasErrorMessage() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"nobody@example.com","password":"pass1234"}
                    """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Invalid credentials"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void errorShape_duplicateEmail_hasErrorMessage() throws Exception {
        String body = """
            {"email":"shape@example.com","password":"pass1234"}
            """;
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Email already registered"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void errorShape_missingToken_hasErrorMessage() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    // --- Refresh tokens ---

    @Test
    void signup_returnsRefreshToken() throws Exception {
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"rt1@example.com","password":"pass1234"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void login_returnsRefreshToken() throws Exception {
        String body = """
            {"email":"rt2@example.com","password":"pass1234"}
            """;
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void refresh_returnsNewTokenPair_whenValid() throws Exception {
        String signupResponse = mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"rt3@example.com","password":"pass1234"}
                    """))
                .andReturn().getResponse().getContentAsString();
        String refreshToken = JsonPath.read(signupResponse, "$.refreshToken");

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void refresh_returns401_whenExpired() throws Exception {
        String signupResponse = mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"rt4@example.com","password":"pass1234"}
                    """))
                .andReturn().getResponse().getContentAsString();
        String refreshToken = JsonPath.read(signupResponse, "$.refreshToken");

        // Expire all refresh tokens directly in the DB — avoids Thread.sleep
        refreshTokenRepository.findAll().forEach(rt -> {
            rt.setExpiresAt(Instant.now().minusSeconds(3600));
            refreshTokenRepository.save(rt);
        });

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_returns401_whenRevoked() throws Exception {
        String signupResponse = mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"rt5@example.com","password":"pass1234"}
                    """))
                .andReturn().getResponse().getContentAsString();
        String refreshToken = JsonPath.read(signupResponse, "$.refreshToken");

        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_returns401_whenInvalidToken() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken":"not-a-real-refresh-token"}
                    """))
                .andExpect(status().isUnauthorized());
    }

    // --- Logout ---

    @Test
    void logout_returns204_whenValid() throws Exception {
        String signupResponse = mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"rt7@example.com","password":"pass1234"}
                    """))
                .andReturn().getResponse().getContentAsString();
        String refreshToken = JsonPath.read(signupResponse, "$.refreshToken");

        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void logout_returns401_whenInvalidToken() throws Exception {
        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"refreshToken":"not-a-real-refresh-token"}
                    """))
                .andExpect(status().isUnauthorized());
    }

    // --- Actuator health ---

    @Test
    void actuatorHealth_returns200_withoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // --- OpenAPI / Swagger ---

    @Test
    void openApiDocs_returns200_withoutAuth() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("/auth/signup")));
    }

    @Test
    void swaggerUi_isReachable() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    // --- CORS ---

    @Test
    void cors_allowedOriginReceivesCorsHeaders() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"cors@example.com","password":"pass1234"}
                    """)
                .header("Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    // --- Refresh-token reuse detection ---

    @Test
    void refresh_reuseOfRotatedToken_revokesEntireTokenFamily() throws Exception {
        // One user, three active sessions (three "devices"): signup + two more logins.
        String signupResponse = mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"reuse@example.com","password":"pass1234"}
                    """))
                .andReturn().getResponse().getContentAsString();
        String tokenA = JsonPath.read(signupResponse, "$.refreshToken");
        String tokenC = loginAndGetRefreshToken("reuse@example.com", "pass1234"); // device 2
        String tokenD = loginAndGetRefreshToken("reuse@example.com", "pass1234"); // device 3

        // First refresh rotates A -> B; A becomes revoked. Active family is now {B, C, D}.
        String refreshResponse = mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + tokenA + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String tokenB = JsonPath.read(refreshResponse, "$.refreshToken");

        // Replaying the rotated token A is detected as reuse -> 401.
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + tokenA + "\"}"))
                .andExpect(status().isUnauthorized());

        // Reuse must revoke the ENTIRE active family, across all devices — B, C and D all die.
        for (String token : new String[]{tokenB, tokenC, tokenD}) {
            mockMvc.perform(post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"refreshToken\":\"" + token + "\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    private String loginAndGetRefreshToken(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.refreshToken");
    }

    // --- Case-insensitive email uniqueness (app-level normalization) ---

    @Test
    void signup_returns409_forCaseVariantEmail() throws Exception {
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"CaseTest@Example.com","password":"pass1234"}
                    """))
                .andExpect(status().isOk());
        // Same address, different case -> normalized to the same value -> duplicate.
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"casetest@example.com","password":"pass1234"}
                    """))
                .andExpect(status().isConflict());
    }

    // --- Logout idempotency ---

    @Test
    void logout_isIdempotent_whenCalledTwice() throws Exception {
        String signupResponse = mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"idemp@example.com","password":"pass1234"}
                    """))
                .andReturn().getResponse().getContentAsString();
        String refreshToken = JsonPath.read(signupResponse, "$.refreshToken");

        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());
        // Revoking an already-revoked token is a no-op -> still 204.
        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());
    }
}
