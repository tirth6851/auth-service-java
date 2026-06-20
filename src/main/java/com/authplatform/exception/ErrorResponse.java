package com.authplatform.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Standard error response returned for all 4xx and 5xx responses")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @Schema(description = "Always false for error responses", example = "false")
        boolean success,

        @Schema(description = "Short, human-readable error message safe to display to end users",
                example = "Invalid credentials")
        String error,

        @Schema(description = "Field-level validation messages — present only for 400 Validation failed responses",
                example = "[\"email: must be a well-formed email address\"]")
        List<String> details
) {

    public static ErrorResponse of(String error) {
        return new ErrorResponse(false, error, null);
    }

    public static ErrorResponse of(String error, List<String> details) {
        return new ErrorResponse(false, error, details);
    }
}
