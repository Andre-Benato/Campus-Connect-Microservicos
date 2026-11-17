package com.campusconnect.security;

import com.campusconnect.apartment.infrastructure.adapters.out.http.TokenValidateResponse;
import com.campusconnect.apartment.infrastructure.adapters.out.http.UserAuthClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica a requisicao chamando o user-service ({@code POST /api/v1/auth/validate}). Nao valida
 * JWT localmente.
 */
@Component
public class RemoteJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserAuthClient userAuthClient;

    public RemoteJwtAuthenticationFilter(UserAuthClient userAuthClient) {
        this.userAuthClient = userAuthClient;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null
                && authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            userAuthClient
                    .validate(authorization)
                    .ifPresent(this::autenticar);
        }

        filterChain.doFilter(request, response);
    }

    private void autenticar(TokenValidateResponse validated) {
        AuthenticatedUser principal =
                new AuthenticatedUser(validated.userId(), validated.email(), validated.name());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
