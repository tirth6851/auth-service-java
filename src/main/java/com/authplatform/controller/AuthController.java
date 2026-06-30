package com.authplatform.controller;

import com.authplatform.dto.AuthResponse;
import com.authplatform.dto.LoginRequest;
import com.authplatform.dto.LogoutRequest;
import com.authplatform.dto.MeResponse;
import com.authplatform.dto.RefreshRequest;
import com.authplatform.dto.SignupRequest;
import com.authplatform.exception.ErrorResponse;
import com.authplatform.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Signup, login, token refresh, and logout")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Register a new user",
            description = "Creates a user account and returns a JWT access token plus a refresh token. " +
                    "Email is normalised (trimmed, lowercased) before storage."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration successful — token pair returned",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed (invalid email or short password)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @Operation(
            summary = "Authenticate an existing user",
            description = "Validates credentials and returns a JWT access token plus a refresh token. " +
                    "Returns 401 for both unknown email and wrong password (user enumeration protection)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful — token pair returned",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed (blank field or invalid email format)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "Rotate a refresh token",
            description = "Exchanges a valid refresh token for a new access + refresh token pair. " +
                    "The submitted token is immediately revoked — always use the latest token. " +
                    "TTL: 7 days."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token pair rotated successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Missing or blank refreshToken field",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token not found, already revoked, or expired",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @Operation(
            summary = "Revoke a refresh token (logout)",
            description = "Revokes the supplied refresh token, ending the session. " +
                    "Idempotent: revoking an already-revoked token returns 204. " +
                    "The active JWT access token is NOT invalidated — it expires naturally."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Refresh token revoked — session ended"),
            @ApiResponse(responseCode = "400", description = "Missing or blank refreshToken field",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token not found in database",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get the current authenticated user",
            description = "Returns the caller's ID, email, verification status, and account creation time. " +
                    "Requires a valid Bearer access token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user returned",
                    content = @Content(schema = @Schema(implementation = MeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing, invalid, or expired access token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.getCurrentUser(authentication.getName()));
    }
}
