package com.authplatform.service;

import com.authplatform.model.RefreshToken;
import com.authplatform.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final long tokenTtlMs;

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${app.refresh-token.ttl-ms:604800000}") long tokenTtlMs) {
        this.repository = repository;
        this.tokenTtlMs = tokenTtlMs;
    }

    public String createToken(Long userId) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = sha256(rawToken);
        repository.save(new RefreshToken(tokenHash, userId, Instant.now().plusMillis(tokenTtlMs)));
        return rawToken;
    }

    @Transactional
    public RefreshToken validateAndRotate(String rawToken) {
        String tokenHash = sha256(rawToken);
        RefreshToken existing = repository.findByToken(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (existing.isRevoked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (existing.isExpired()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        existing.setRevokedAt(Instant.now());
        repository.save(existing);
        return existing;
    }

    public void revokeToken(String rawToken) {
        String tokenHash = sha256(rawToken);
        RefreshToken token = repository.findByToken(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (token.getRevokedAt() == null) {
            token.setRevokedAt(Instant.now());
            repository.save(token);
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
