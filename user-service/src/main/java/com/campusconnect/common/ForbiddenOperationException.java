package com.campusconnect.common;

/**
 * Lancada quando o usuario esta autenticado, mas a operacao nao pertence a ele. Mapeada para HTTP
 * 403.
 */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
