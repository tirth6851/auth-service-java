package com.authplatform.config;

import com.authplatform.security.AgentAuthenticationFilter;
import com.authplatform.security.Http401UnauthorizedEntryPoint;
import com.authplatform.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AgentAuthenticationFilter agentAuthenticationFilter;
    private final Http401UnauthorizedEntryPoint unauthorizedEntryPoint;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          AgentAuthenticationFilter agentAuthenticationFilter,
                          Http401UnauthorizedEntryPoint unauthorizedEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.agentAuthenticationFilter = agentAuthenticationFilter;
        this.unauthorizedEntryPoint = unauthorizedEntryPoint;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/signup", "/auth/login", "/auth/refresh", "/auth/logout",
                                "/h2-console/**", "/error", "/actuator/health",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
                        ).permitAll()
                        // /auth/me intentionally NOT permitAll — requires a valid Bearer token
                        .anyRequest().authenticated()
                )
                .exceptionHandling(eh -> eh.authenticationEntryPoint(unauthorizedEntryPoint))
                // SAMEORIGIN: allows H2 console frames locally while blocking external framing
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                // Agent filter runs before the JWT filter; each acts only on its own token
                // prefix (ak_ vs a JWT) and sets auth only when none is present yet.
                .addFilterBefore(agentAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
