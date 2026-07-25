package com.authplatform.repository;

import com.authplatform.model.RefreshToken;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the {@code @Version} optimistic lock on {@link RefreshToken} actually fires — i.e. two
 * concurrent rotations of the same token cannot both commit. Without this test the lock is invisible
 * to the suite: the exception→401 mapping is tested elsewhere in isolation, but nothing forced JPA to
 * throw the exception in the first place (removing {@code @Version} would otherwise leave everything
 * green).
 *
 * <p>The conflict is reproduced deterministically inside one transaction using detach/clear to
 * simulate a second, stale copy of the row (equivalent to a second concurrent transaction that read
 * the row before the first committed).
 */
@DataJpaTest
class RefreshTokenOptimisticLockTest {

    @Autowired RefreshTokenRepository repository;
    @Autowired EntityManager em;

    @Test
    void versionIsInitializedOnPersist() {
        RefreshToken saved = repository.saveAndFlush(
                new RefreshToken("hash-version-init", 1L, Instant.now().plusSeconds(3600)));
        assertThat(saved.getVersion()).isNotNull();
    }

    @Test
    void staleConcurrentRotation_losesOptimisticLockRace() {
        RefreshToken saved = repository.saveAndFlush(
                new RefreshToken("hash-optlock", 1L, Instant.now().plusSeconds(3600)));
        Long id = saved.getId();
        em.flush();
        em.clear();

        // "Transaction 1" reads the row (version 0), then is set aside (detached) — a stale copy.
        RefreshToken first = repository.findById(id).orElseThrow();
        em.detach(first);

        // "Transaction 2" rotates the same token: version 0 -> 1 in the database.
        RefreshToken second = repository.findById(id).orElseThrow();
        second.setRevokedAt(Instant.now());
        repository.saveAndFlush(second);
        em.clear();

        // Transaction 1 now tries to rotate its stale copy — must lose the race, not silently win.
        first.setRevokedAt(Instant.now());
        assertThatThrownBy(() -> repository.saveAndFlush(first))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }
}
