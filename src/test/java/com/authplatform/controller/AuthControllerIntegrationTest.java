package com.authplatform.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

        // Extract token from the signup response JSON
        String token = JsonPath.read(signupResponse, "$.token");

        // A valid token passes authentication; /api/protected doesn't exist so Spring returns 404,
        // confirming the request was not rejected by the security layer.
        mockMvc.perform(get("/api/protected")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
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

    // --- Error response shape tests ---

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
}
