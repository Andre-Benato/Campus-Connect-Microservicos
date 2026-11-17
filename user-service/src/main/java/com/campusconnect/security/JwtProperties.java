package com.campusconnect.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuracao dos tokens JWT (RF02).
 *
 * <p>Todos os valores possuem um default em {@code application.properties} e podem ser sobrescritos
 * por variavel de ambiente ({@code CAMPUSCONNECT_JWT_SECRET}, {@code CAMPUSCONNECT_JWT_EXPIRATION},
 * {@code CAMPUSCONNECT_JWT_ISSUER} e {@code CAMPUSCONNECT_JWT_CLEANUP_INTERVAL}).
 */
@Validated
@ConfigurationProperties(prefix = "campusconnect.security.jwt")
public class JwtProperties {

    /** Valor de desenvolvimento; em producao deve ser substituido por variavel de ambiente. */
    public static final String DEFAULT_DEV_SECRET =
            "campus-connect-dev-jwt-secret-trocar-em-producao";

    /**
     * Chave usada para assinar e verificar os tokens (HMAC-SHA256).
     *
     * <p>O HS256 exige uma chave de pelo menos 256 bits, ou seja, 32 caracteres.
     */
    @NotBlank
    @Size(min = 32, message = "o secret do JWT precisa ter no minimo 32 caracteres (HS256)")
    private String secret = DEFAULT_DEV_SECRET;

    /** Tempo de vida do token. O plano de arquitetura define 5 horas. */
    @NotNull
    private Duration expiration = Duration.ofHours(5);

    /** Identificador da aplicacao gravado na claim {@code iss} e validado na entrada. */
    @NotBlank
    private String issuer = "campus-connect";

    /** Intervalo entre as execucoes da limpeza de tokens revogados que ja expiraram. */
    @NotNull
    private Duration cleanupInterval = Duration.ofHours(1);

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getExpiration() {
        return expiration;
    }

    public void setExpiration(Duration expiration) {
        this.expiration = expiration;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    public void setCleanupInterval(Duration cleanupInterval) {
        this.cleanupInterval = cleanupInterval;
    }
}
