package com.authplatform.security;

/**
 * Authenticated principal placed in the SecurityContext by {@link JwtAuthenticationFilter}.
 *
 * <p>Carries both the user id (the JWT subject, per ADR-001) and the email claim so downstream
 * code — e.g. a future {@code GET /auth/me} — can read identity without a DB round-trip.
 * Authorities are attached separately on the {@code Authentication}; when RBAC lands, roles from
 * the JWT will populate them.
 */
public record AuthenticatedUser(Long userId, String email) {
}
