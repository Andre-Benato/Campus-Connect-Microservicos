package com.campusconnect.apartment.infrastructure.adapters.out.http;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Chama o user-service para validar o Bearer token. Qualquer falha (401, 5xx, rede) significa token
 * invalido do ponto de vista deste microservico.
 */
@Component
public class UserAuthClient {

    private static final Logger log = LoggerFactory.getLogger(UserAuthClient.class);

    private final RestClient restClient;

    public UserAuthClient(RestClient.Builder restClientBuilder, UserServiceProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
    }

    public Optional<TokenValidateResponse> validate(String authorizationHeader) {
        try {
            TokenValidateResponse body =
                    restClient
                            .post()
                            .uri("/api/v1/auth/validate")
                            .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                            .retrieve()
                            .body(TokenValidateResponse.class);
            return Optional.ofNullable(body);
        } catch (RestClientResponseException e) {
            log.debug("user-service rejeitou o token: status={}", e.getStatusCode().value());
            return Optional.empty();
        } catch (RestClientException e) {
            log.warn("Falha ao chamar user-service para validar token: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
