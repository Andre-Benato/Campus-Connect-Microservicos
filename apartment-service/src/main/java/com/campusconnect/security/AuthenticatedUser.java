package com.campusconnect.security;

/**
 * Principal autenticado apos validacao remota no user-service.
 *
 * @param id id do usuario no user-service
 */
public record AuthenticatedUser(Long id, String email, String name) {}
