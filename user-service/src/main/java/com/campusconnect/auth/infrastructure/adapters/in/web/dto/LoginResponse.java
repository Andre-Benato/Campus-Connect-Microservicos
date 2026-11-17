package com.campusconnect.auth.infrastructure.adapters.in.web.dto;

import com.campusconnect.user.infrastructure.adapters.in.web.dto.UserResponse;
import java.time.Instant;

/**
 * Resposta do login.
 *
 * @param accessToken token JWT que deve ser enviado no cabecalho {@code Authorization: Bearer ...}
 * @param tokenType esquema de autenticacao esperado pelo servidor
 * @param expiresIn tempo de vida do token em segundos
 * @param expiresAt momento exato em que o token deixa de valer
 * @param user dados publicos do usuario autenticado
 */
public record LoginResponse(
        String accessToken, String tokenType, long expiresIn, Instant expiresAt, UserResponse user) {

    public static final String BEARER = "Bearer";
}
