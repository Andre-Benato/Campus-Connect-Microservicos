package com.campusconnect.common;

/** Lancada quando o e-mail informado ja pertence a outro usuario. Mapeada para HTTP 409. */
public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String email) {
        super("O e-mail '" + email + "' ja esta cadastrado");
    }
}
