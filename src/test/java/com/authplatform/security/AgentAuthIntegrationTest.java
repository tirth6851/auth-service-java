package com.authplatform.security;

import com.authplatform.model.AgentClient;
import com.authplatform.repository.AgentClientRepository;
import com.authplatform.service.AgentClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end auth checks for {@code AgentAuthenticationFilter}. A protected path
 * that has no controller returns 404 once the request passes the security layer,
 * so 404 = "authenticated", 401 = "rejected" — the same convention the JWT
 * protected-route tests use.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AgentAuthIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired AgentClientService agentClientService;
    @Autowired AgentClientRepository agentClientRepository;

    @Test
    void protectedRoute_passesAuth_withValidAgentKey() throws Exception {
        String rawKey = agentClientService.createClient("it-agent", Set.of("users:read")).rawKey();

        mockMvc.perform(get("/api/protected")
                        .header("Authorization", "Bearer " + rawKey))
                .andExpect(status().isNotFound());
    }

    @Test
    void protectedRoute_returns401_withNoKey() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_returns401_withUnknownAgentKey() throws Exception {
        mockMvc.perform(get("/api/protected")
                        .header("Authorization", "Bearer ak_live_unknownkeyvalue"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_returns401_withRevokedAgentKey() throws Exception {
        AgentClientService.CreatedAgentClient created =
                agentClientService.createClient("revoked-agent", Set.of("users:read"));
        AgentClient client = agentClientRepository.findById(created.id()).orElseThrow();
        client.setRevokedAt(Instant.now());
        agentClientRepository.save(client);

        mockMvc.perform(get("/api/protected")
                        .header("Authorization", "Bearer " + created.rawKey()))
                .andExpect(status().isUnauthorized());
    }
}
