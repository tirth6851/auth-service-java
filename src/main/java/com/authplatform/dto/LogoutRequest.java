package com.authplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Logout request — revokes the supplied refresh token")
public class LogoutRequest {

    @Schema(description = "Refresh token UUID to revoke",
            example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    public LogoutRequest() {}

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
