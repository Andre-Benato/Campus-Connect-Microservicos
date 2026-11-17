package com.campusconnect.security;

import org.springframework.security.oauth2.jwt.Jwt;

import com.campusconnect.auth.application.JwtService;
/**
 * Leitura do usuario dono do token da requisicao.
 *
 * <p>O id vai na claim {@code sub} no momento da emissao ({@code JwtService.emitir}); aqui ele volta
 * para {@code Long}.
 */
public final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    public static Long id(Jwt token) {
        return Long.valueOf(token.getSubject());
    }
}
