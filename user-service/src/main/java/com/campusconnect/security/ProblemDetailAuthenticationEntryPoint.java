package com.campusconnect.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

/**
 * Resposta 401 para requisicoes sem token, com token invalido, expirado ou ja invalidado pelo
 * logout.
 */
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final BearerTokenAuthenticationEntryPoint delegate =
            new BearerTokenAuthenticationEntryPoint();
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
        // O delegate cuida do cabecalho WWW-Authenticate exigido pelo esquema Bearer.
        delegate.commence(request, response, authException);

        writer.escrever(
                response,
                HttpStatus.UNAUTHORIZED,
                "Nao autenticado",
                "E necessario um token de acesso valido no cabecalho Authorization");
    }
}
