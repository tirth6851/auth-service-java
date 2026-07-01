package com.authplatform.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A machine client (AI agent) that authenticates with a scoped API key.
 * Distinct from {@link User}: no password/email, scopes instead of roles,
 * a separate lifecycle. The raw key is never stored — only its SHA-256 hash,
 * matching the refresh-token pattern.
 */
@Entity
@Table(name = "agent_clients")
public class AgentClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String keyHash;

    // EAGER so the authentication filter can read scopes outside a transaction.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "agent_client_scopes",
            joinColumns = @JoinColumn(name = "agent_client_id"))
    @Column(name = "scope", nullable = false)
    private Set<String> scopes = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant lastUsedAt;

    @Column
    private Instant revokedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public AgentClient() {}

    public AgentClient(String name, String keyHash, Set<String> scopes) {
        this.name = name;
        this.keyHash = keyHash;
        this.scopes = scopes;
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getKeyHash() { return keyHash; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }

    public Set<String> getScopes() { return scopes; }
    public void setScopes(Set<String> scopes) { this.scopes = scopes; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

    public boolean isRevoked() {
        return revokedAt != null;
    }
}
