package com.authplatform.service;

import com.authplatform.model.RefreshToken;
import com.authplatform.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final long TTL_MS = 604_800_000L; // 7 days

    @Mock RefreshTokenRepository repository;
    @Mock RefreshTokenReuseHandler reuseHandler;

    RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, reuseHandler, TTL_MS);
    }

    private static String sha256(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createToken_persistsHashNotRaw_andReturnsRawToken() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String raw = service.createToken(9L);

        verify(repository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(9L);
        assertThat(saved.getToken()).hasSize(64);                 // SHA-256 hex
        assertThat(saved.getToken()).isNotEqualTo(raw);           // stored hashed, not raw
        assertThat(saved.getToken()).isEqualTo(sha256(raw));      // stored value is the hash of raw
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());  // TTL in the future
    }

    @Test
    void validateAndRotate_validToken_revokesOldAndReturnsIt() {
        String raw = "the-raw-token";
        RefreshToken active = new RefreshToken(sha256(raw), 5L, Instant.now().plusSeconds(3600));
        when(repository.findByToken(sha256(raw))).thenReturn(Optional.of(active));

        RefreshToken result = service.validateAndRotate(raw);

        assertThat(result).isSameAs(active);
        assertThat(result.getRevokedAt()).isNotNull();          // old token now revoked (rotated)
        verify(repository).save(active);
        verify(reuseHandler, never()).revokeFamily(any());      // not a reuse
    }

    @Test
    void validateAndRotate_unknownToken_throws401_andNoFamilyRevoke() {
        when(repository.findByToken(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateAndRotate("missing"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
        verify(reuseHandler, never()).revokeFamily(any());
    }

    @Test
    void validateAndRotate_expiredToken_throws401_andNoFamilyRevoke() {
        String raw = "expired-token";
        RefreshToken expired = new RefreshToken(sha256(raw), 5L, Instant.now().minusSeconds(3600));
        when(repository.findByToken(sha256(raw))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.validateAndRotate(raw))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
        verify(reuseHandler, never()).revokeFamily(any());      // expiry is not theft
    }

    @Test
    void validateAndRotate_revokedTokenReuse_revokesFamilyAndThrows401() {
        String raw = "reused-token";
        RefreshToken revoked = new RefreshToken(sha256(raw), 7L, Instant.now().plusSeconds(3600));
        revoked.setRevokedAt(Instant.now().minusSeconds(60));   // already rotated/logged out
        when(repository.findByToken(sha256(raw))).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.validateAndRotate(raw))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
        verify(reuseHandler).revokeFamily(7L);                  // whole family killed
    }

    @Test
    void revokeToken_activeToken_setsRevokedAt() {
        String raw = "logout-token";
        RefreshToken active = new RefreshToken(sha256(raw), 3L, Instant.now().plusSeconds(3600));
        when(repository.findByToken(sha256(raw))).thenReturn(Optional.of(active));

        service.revokeToken(raw);

        assertThat(active.getRevokedAt()).isNotNull();
        verify(repository).save(active);
    }

    @Test
    void revokeToken_alreadyRevoked_isIdempotent_noSecondSave() {
        String raw = "already-out";
        RefreshToken revoked = new RefreshToken(sha256(raw), 3L, Instant.now().plusSeconds(3600));
        revoked.setRevokedAt(Instant.now().minusSeconds(60));
        when(repository.findByToken(sha256(raw))).thenReturn(Optional.of(revoked));

        service.revokeToken(raw);

        verify(repository, never()).save(any());                // no-op on already-revoked
    }

    @Test
    void revokeToken_unknownToken_throws401() {
        when(repository.findByToken(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revokeToken("nope"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }
}
