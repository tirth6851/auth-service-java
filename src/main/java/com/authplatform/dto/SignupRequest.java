package com.authplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Signup request body")
public class SignupRequest {

    @Schema(description = "Valid email address (trimmed and lowercased before storage)", example = "alice@example.com")
    @NotBlank
    @Email
    private String email;

    @Schema(description = "Password — minimum 8 characters", example = "Str0ngPass!")
    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    public SignupRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
