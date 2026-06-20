package com.authplatform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Successful authentication response containing a token pair")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    @Schema(description = "HS256 JWT access token (default TTL: 1 hour)", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Token type — always 'Bearer'", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "Opaque refresh token UUID (TTL: 7 days, rotated on every /auth/refresh call). " +
            "Null when returned from /auth/refresh for a non-refresh flow.",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private String refreshToken;

    public AuthResponse() {}

    public AuthResponse(String token) {
        this.token = token;
    }

    public AuthResponse(String token, String refreshToken) {
        this.token = token;
        this.refreshToken = refreshToken;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
