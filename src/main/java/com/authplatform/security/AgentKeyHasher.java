package com.authplatform.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hashes raw agent API keys for storage and lookup. Uses SHA-256 (hex) — the
 * same scheme {@code RefreshTokenService} applies to refresh tokens, so hashes
 * fit the same VARCHAR(64) column width. Keys are high-entropy random secrets,
 * so a fast one-way hash is appropriate (unlike passwords, which need BCrypt).
 */
@Component
public class AgentKeyHasher {

    public String hash(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
