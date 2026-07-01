-- created_at has no DB default; set by AgentClient @PrePersist, matching V1/V2 pattern.
CREATE TABLE agent_clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    key_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP NULL,
    revoked_at TIMESTAMP NULL,
    UNIQUE (key_hash)
);

-- Scopes as a side table (@ElementCollection). Composite PK dedupes scopes per client.
CREATE TABLE agent_client_scopes (
    agent_client_id BIGINT NOT NULL REFERENCES agent_clients(id) ON DELETE CASCADE,
    scope VARCHAR(64) NOT NULL,
    PRIMARY KEY (agent_client_id, scope)
);
