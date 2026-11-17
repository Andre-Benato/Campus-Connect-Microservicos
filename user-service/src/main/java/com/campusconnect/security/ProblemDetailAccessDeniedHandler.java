package com.campusconnect.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

/** Resposta 403 para requisicoes autenticadas que nao possuem permissao sobre o recurso. */
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailWriter writer;

    public ProblemDetailAccessDeniedHandler(ObjectMapper objectMapper) {
        this.writer = new ProblemDetailWriter(objectMapper);
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        writer.escrever(
                response,
                HttpStatus.FORBIDDEN,
                "Acesso negado",
                "O usuario autenticado nao tem permissao para esta operacao");
    }
}
