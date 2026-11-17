package com.campusconnect.user.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados de entrada para a atualizacao de um usuario.
 *
 * <p>O campo {@code password} e opcional: quando nulo, a senha atual e mantida.
 */
public record UserUpdateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 180) String email,
        @Size(min = 8, max = 72) String password) {
}
