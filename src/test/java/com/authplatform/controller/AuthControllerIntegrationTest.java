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
}
