package com.authplatform.exception;

import com.authplatform.model.RefreshToken;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleOptimisticLock_mapsToInvalidCredentials() {
        ErrorResponse body = handler.handleOptimisticLock(
                new ObjectOptimisticLockingFailureException(RefreshToken.class, 1L));

        assertThat(body.success()).isFalse();
        assertThat(body.error()).isEqualTo("Invalid credentials"); // non-revealing, same shape as other 401s
        assertThat(body.details()).isNull();
    }

    @Test
    void handleGeneric_hidesInternalDetails() {
        ErrorResponse body = handler.handleGeneric(new RuntimeException("stack trace with secrets"));

        assertThat(body.success()).isFalse();
        assertThat(body.error()).isEqualTo("An unexpected error occurred"); // does not leak the cause
        assertThat(body.details()).isNull();
    }
}
