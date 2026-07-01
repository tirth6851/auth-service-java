package com.authplatform.repository;

import com.authplatform.model.AgentClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentClientRepository extends JpaRepository<AgentClient, Long> {
    Optional<AgentClient> findByKeyHash(String keyHash);
    boolean existsByKeyHash(String keyHash);
}
