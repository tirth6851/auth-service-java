package com.authplatform.config;

import com.authplatform.repository.AgentClientRepository;
import com.authplatform.security.AgentKeyHasher;
import com.authplatform.model.AgentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Seeds a single admin agent client (scope {@code *}) from the raw key in
 * {@code AGENT_BOOTSTRAP_KEY} so the first agent can be provisioned before any
 * agent-management endpoint exists. Idempotent: skips if the key is unset or a
 * client with that hash already exists. Never logs the raw key.
 */
@Component
public class AgentClientBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AgentClientBootstrap.class);
    private static final String ADMIN_NAME = "bootstrap-admin";

    private final AgentClientRepository repository;
    private final AgentKeyHasher hasher;
    private final String bootstrapKey;

    public AgentClientBootstrap(AgentClientRepository repository,
                                AgentKeyHasher hasher,
                                @Value("${app.agent.bootstrap-key:}") String bootstrapKey) {
        this.repository = repository;
        this.hasher = hasher;
        this.bootstrapKey = bootstrapKey;
    }

    @Override
    public void run(String... args) {
        if (bootstrapKey == null || bootstrapKey.isBlank()) {
            log.info("No AGENT_BOOTSTRAP_KEY set; skipping admin agent client seeding.");
            return;
        }
        String keyHash = hasher.hash(bootstrapKey);
        if (repository.existsByKeyHash(keyHash)) {
            log.info("Bootstrap admin agent client already present; nothing to seed.");
            return;
        }
        repository.save(new AgentClient(ADMIN_NAME, keyHash, Set.of("*")));
        log.info("Seeded bootstrap admin agent client '{}' with scope '*'.", ADMIN_NAME);
    }
}
