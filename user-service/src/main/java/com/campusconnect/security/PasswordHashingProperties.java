package com.campusconnect.security;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuracao do hash de senha.
 *
 * <p>Os valores possuem um default em {@code application.properties} e podem ser sobrescritos por
 * variavel de ambiente ({@code CAMPUSCONNECT_PASSWORD_SECRET} e
 * {@code CAMPUSCONNECT_PASSWORD_STRENGTH}).
 */
@Validated
@ConfigurationProperties(prefix = "campusconnect.security.password")
public class PasswordHashingProperties {

    /** Valor de desenvolvimento; em producao deve ser substituido por variavel de ambiente. */
    public static final String DEFAULT_DEV_SECRET = "campus-connect-dev-secret-trocar-em-producao";

    /** Secret (pepper) combinado com a senha antes do BCrypt. */
    @NotBlank
    private String secret = DEFAULT_DEV_SECRET;

    /** Custo do BCrypt (numero de rounds = 2^strength). */
    @Min(4)
    @Max(31)
    private int strength = 10;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }
}
