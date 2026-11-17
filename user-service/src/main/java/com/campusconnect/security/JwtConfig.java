package com.campusconnect.security;

import com.campusconnect.auth.infrastructure.adapters.out.persistence.RevokedTokenRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Emissao e verificacao dos tokens JWT (RF02).
 *
 * <p>A assinatura usa HMAC-SHA256 com o secret da aplicacao: como emissor e verificador sao o mesmo
 * servico, uma chave simetrica e suficiente e evita o custo de gerenciar um par de chaves.
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    /** Algoritmo de assinatura dos tokens. */
    public static final MacAlgorithm ALGORITMO = MacAlgorithm.HS256;

    /** Nome JCA correspondente ao {@link #ALGORITMO}, usado para montar a chave simetrica. */
    private static final String ALGORITMO_JCA = "HmacSHA256";

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties properties) {
        if (JwtProperties.DEFAULT_DEV_SECRET.equals(properties.getSecret())) {
            log.warn(
                    "Usando o secret de JWT default. Defina CAMPUSCONNECT_JWT_SECRET antes de implantar"
                            + " em producao.");
        }
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave(properties)));
    }

    /**
     * Alem da assinatura, o decoder valida a expiracao, o emissor e a denylist de logout. Qualquer
     * uma dessas verificacoes falhando resulta em 401 antes de a requisicao chegar ao controller.
     */
    @Bean
    public JwtDecoder jwtDecoder(
            JwtProperties properties, RevokedTokenRepository revokedTokenRepository) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withSecretKey(chave(properties)).macAlgorithm(ALGORITMO).build();

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<Jwt>(
                        new JwtTimestampValidator(),
                        new JwtIssuerValidator(properties.getIssuer()),
                        new RevokedTokenValidator(revokedTokenRepository)));

        return decoder;
    }

    private static SecretKey chave(JwtProperties properties) {
        return new SecretKeySpec(
                properties.getSecret().getBytes(StandardCharsets.UTF_8), ALGORITMO_JCA);
    }
}
