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
}
