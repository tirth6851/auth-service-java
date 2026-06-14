package com.authplatform.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(boolean success, String error, List<String> details) {

    public static ErrorResponse of(String error) {
        return new ErrorResponse(false, error, null);
    }

    public static ErrorResponse of(String error, List<String> details) {
        return new ErrorResponse(false, error, details);
    }
}
