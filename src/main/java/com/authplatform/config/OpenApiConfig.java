package com.authplatform.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Auth Platform API",
                version = "1.0",
                description = "Stateless JWT authentication service. Provides signup, login, " +
                        "token refresh, and logout. Access tokens are HS256 JWTs (1-hour TTL). " +
                        "Refresh tokens are opaque UUIDs (7-day TTL, rotated on every use)."
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Paste the access token returned by /auth/signup or /auth/login. " +
                "Format: 'Bearer eyJhbGci...'"
)
public class OpenApiConfig {
}
