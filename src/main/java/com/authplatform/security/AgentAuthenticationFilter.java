package com.authplatform.security;

import com.authplatform.model.AgentClient;
import com.authplatform.service.AgentClientService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates agent clients presenting {@code Authorization: Bearer ak_live_...}.
 * The {@code ak_} prefix distinguishes agent keys from user JWTs, so this filter
 * only touches agent keys and leaves JWTs to {@link JwtAuthenticationFilter}.
 * Runs before the JWT filter; each sets auth only when none is present yet.
 * Scopes become granted authorities; enforcement (@PreAuthorize) comes in Phase C.
 */
@Component
public class AgentAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String BEARER = "Bearer ";

    private final AgentClientService agentClientService;

    public AgentAuthenticationFilter(AgentClientService agentClientService) {
        this.agentClientService = agentClientService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(BEARER)) {
            String token = header.substring(BEARER.length());
            if (token.startsWith(AgentClientService.KEY_PREFIX)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    agentClientService.authenticate(token).ifPresent(client ->
                            SecurityContextHolder.getContext().setAuthentication(toAuthentication(client, request)));
                } catch (Exception ignored) {
                    // invalid key — continue unauthenticated, entry point returns 401
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken toAuthentication(AgentClient client, HttpServletRequest request) {
        List<SimpleGrantedAuthority> authorities = client.getScopes().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(client.getName(), null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return auth;
    }
}
