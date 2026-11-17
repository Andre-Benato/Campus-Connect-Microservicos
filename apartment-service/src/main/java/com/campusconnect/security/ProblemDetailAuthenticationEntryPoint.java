package com.campusconnect.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailWriter writer;

    public ProblemDetailAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.writer = new ProblemDetailWriter(objectMapper);
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        writer.escrever(
                response,
                HttpStatus.UNAUTHORIZED,
                "Nao autenticado",
                "E necessario um token de acesso valido no cabecalho Authorization");
    }
}
