package com.authplatform.service;

import com.authplatform.model.AgentClient;
import com.authplatform.repository.AgentClientRepository;
import com.authplatform.security.AgentKeyHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

/**
 * Provisions and authenticates agent clients. The raw API key is returned
 * exactly once, at creation — only its hash is persisted.
 */
@Service
public class AgentClientService {

    /** Public prefix so keys are recognizable and the filter can route on it. */
    public static final String KEY_PREFIX = "ak_live_";

    private static final int KEY_BYTES = 32;

    private final AgentClientRepository repository;
    private final AgentKeyHasher hasher;
    private final SecureRandom secureRandom = new SecureRandom();

    public AgentClientService(AgentClientRepository repository, AgentKeyHasher hasher) {
        this.repository = repository;
        this.hasher = hasher;
    }

    /** The raw key is present only in this result — it is never stored or recoverable later. */
    public record CreatedAgentClient(Long id, String name, Set<String> scopes, String rawKey) {}

    @Transactional
    public CreatedAgentClient createClient(String name, Set<String> scopes) {
        String rawKey = generateRawKey();
        AgentClient client = new AgentClient(name, hasher.hash(rawKey), Set.copyOf(scopes));
        AgentClient saved = repository.save(client);
        return new CreatedAgentClient(saved.getId(), saved.getName(), saved.getScopes(), rawKey);
    }

    /**
     * Resolves a raw key to a live (non-revoked) client, recording last-used time.
     * Returns empty for unknown or revoked keys — callers must not distinguish the two.
     */
    @Transactional
    public Optional<AgentClient> authenticate(String rawKey) {
        return repository.findByKeyHash(hasher.hash(rawKey))
                .filter(client -> !client.isRevoked())
                .map(client -> {
                    client.setLastUsedAt(Instant.now());
                    return repository.save(client);
                });
    }

    private String generateRawKey() {
        byte[] bytes = new byte[KEY_BYTES];
        secureRandom.nextBytes(bytes);
        return KEY_PREFIX + HexFormat.of().formatHex(bytes);
    }
}
