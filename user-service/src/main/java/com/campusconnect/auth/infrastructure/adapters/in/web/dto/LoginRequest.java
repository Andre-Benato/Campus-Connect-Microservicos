package com.campusconnect.auth.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Credenciais informadas no login (RF02). */
public record LoginRequest(
        @NotBlank @Email @Size(max = 180) String email, @NotBlank @Size(max = 72) String password) {
}
