package com.campusconnect.common;

/**
 * Lancada quando o e-mail nao existe ou a senha esta incorreta. Mapeada para HTTP 401.
 *
 * <p>A mensagem e deliberadamente generica: distinguir "e-mail inexistente" de "senha incorreta"
 * permitiria descobrir quais e-mails possuem cadastro na plataforma.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("E-mail ou senha invalidos");
    }
}
