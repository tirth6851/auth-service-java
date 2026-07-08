package com.authplatform.service;

import com.authplatform.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Handles refresh-token reuse detection.
 *
 * <p>When a refresh token that has already been revoked (rotated away or logged out) is
 * replayed, that is a strong signal the token was stolen. The safe response is to revoke the
 * user's entire active token family so the attacker's copy — and the legitimate user's — are
 * both invalidated, forcing a fresh login.
 *
 * <p>The revocation runs in its own {@code REQUIRES_NEW} transaction so it is committed even
 * though the triggering request is aborted with 401 (which rolls back the request transaction).
 * This must live in a separate bean: a self-invocation would bypass the Spring proxy and the
 * new transaction would never start.
 */
@Service
public class RefreshTokenReuseHandler {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenReuseHandler.class);

    private final RefreshTokenRepository repository;

    public RefreshTokenReuseHandler(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeFamily(Long userId) {
        int revoked = repository.revokeAllActiveByUserId(userId, Instant.now());
        log.warn("Refresh-token reuse detected for userId={}; revoked {} active token(s)", userId, revoked);
    }
}
