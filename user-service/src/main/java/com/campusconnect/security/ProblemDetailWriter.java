package com.campusconnect.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

/**
 * Escreve respostas de erro no formato Problem Details (RFC 9457) direto na resposta HTTP.
 *
 * <p>Erros de seguranca acontecem nos filtros, antes de a requisicao chegar a um controller, e por
 * isso nao passam pelo {@code @RestControllerAdvice}. Este utilitario mantem o formato dessas
 * respostas igual ao do restante da API.
 */
final class ProblemDetailWriter {

    private final ObjectMapper objectMapper;

    ProblemDetailWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void escrever(HttpServletResponse response, HttpStatus status, String title, String detail)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
