package com.authplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Refresh token rotation request")
public class RefreshRequest {

    @Schema(description = "Current refresh token UUID obtained from signup or login (or a prior refresh)",
            example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    public RefreshRequest() {}

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
