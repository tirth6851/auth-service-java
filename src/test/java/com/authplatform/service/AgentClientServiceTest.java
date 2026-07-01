package com.authplatform.service;

import com.authplatform.model.AgentClient;
import com.authplatform.repository.AgentClientRepository;
import com.authplatform.security.AgentKeyHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AgentClientServiceTest {

    private AgentClientRepository repository;
    private AgentKeyHasher hasher;
    private AgentClientService service;

    @BeforeEach
    void setUp() {
        repository = mock(AgentClientRepository.class);
        hasher = new AgentKeyHasher();
        service = new AgentClientService(repository, hasher);
        // save() returns its argument so the service can read back the persisted entity
        when(repository.save(any(AgentClient.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createClient_returnsRawKeyWithPrefix_andStoresOnlyTheHash() {
        AgentClientService.CreatedAgentClient created =
                service.createClient("support-agent", Set.of("users:read"));

        assertThat(created.rawKey()).startsWith(AgentClientService.KEY_PREFIX);
        assertThat(created.scopes()).containsExactly("users:read");

        // The persisted entity holds the hash of the raw key, never the raw key itself
        verify(repository).save(argThat(c ->
                c.getKeyHash().equals(hasher.hash(created.rawKey()))
                        && !c.getKeyHash().equals(created.rawKey())));
    }

    @Test
    void authenticate_returnsClient_forKnownLiveKey() {
        String rawKey = service.createClient("a", Set.of("users:read")).rawKey();
        AgentClient stored = new AgentClient("a", hasher.hash(rawKey), Set.of("users:read"));
        when(repository.findByKeyHash(hasher.hash(rawKey))).thenReturn(Optional.of(stored));

        assertThat(service.authenticate(rawKey)).containsSame(stored);
    }

    @Test
    void authenticate_returnsEmpty_forUnknownKey() {
        when(repository.findByKeyHash(any())).thenReturn(Optional.empty());
        assertThat(service.authenticate("ak_live_deadbeef")).isEmpty();
    }

    @Test
    void authenticate_returnsEmpty_forRevokedKey() {
        String rawKey = "ak_live_revoked";
        AgentClient revoked = new AgentClient("a", hasher.hash(rawKey), Set.of("users:read"));
        revoked.setRevokedAt(java.time.Instant.now());
        when(repository.findByKeyHash(hasher.hash(rawKey))).thenReturn(Optional.of(revoked));

        assertThat(service.authenticate(rawKey)).isEmpty();
    }
}
