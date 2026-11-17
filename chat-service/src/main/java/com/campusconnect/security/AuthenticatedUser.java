package com.campusconnect.security;

import java.security.Principal;

/**
 * Principal autenticado apos validacao remota no user-service.
 *
 * <p>{@link #getName()} retorna o id como String para destinos STOMP {@code /user/...}.
 *
 * @param id id do usuario no user-service
 * @param email email do usuario
 * @param name nome de exibicao
 */
public record AuthenticatedUser(Long id, String email, String name) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(id);
    }
}
