package com.campusconnect.auth.infrastructure.adapters.in.web.dto;

import org.springframework.security.oauth2.jwt.Jwt;

/** Resposta da validacao remota de token para outros microservicos. */
public record TokenValidateResponse(Long userId, String email, String name) {

    public static TokenValidateResponse from(Jwt token) {
        return new TokenValidateResponse(
                Long.valueOf(token.getSubject()),
                token.getClaimAsString("email"),
                token.getClaimAsString("name"));
    }
}
