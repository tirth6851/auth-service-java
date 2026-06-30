package com.authplatform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login request body")
public class LoginRequest {

    @Schema(description = "Registered email address", example = "alice@example.com")
    @NotBlank
    @Email
    private String email;

    @Schema(description = "Account password", example = "Str0ngPass!")
    @NotBlank
    private String password;

    public LoginRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
