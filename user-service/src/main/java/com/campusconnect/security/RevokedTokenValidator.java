package com.campusconnect.security;

import com.campusconnect.auth.infrastructure.adapters.out.persistence.RevokedTokenRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Recusa tokens que passaram pelo logout (RF02).
 *
 * <p>Roda junto com as validacoes de assinatura, emissor e expiracao, durante a decodificacao do
 * token. Um token sem a claim {@code jti} tambem e recusado: todos os tokens emitidos pela
 * aplicacao possuem esse identificador, e sem ele nao ha como revogar o token depois.
 */
public class RevokedTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error TOKEN_REVOGADO =
            new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_TOKEN, "O token foi invalidado por logout", null);

    private static final OAuth2Error SEM_IDENTIFICADOR =
            new OAuth2Error(
                    OAuth2ErrorCodes.INVALID_TOKEN, "O token nao possui a claim 'jti'", null);

    private final RevokedTokenRepository revokedTokenRepository;

    public RevokedTokenValidator(RevokedTokenRepository revokedTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String tokenId = token.getId();
        if (tokenId == null || tokenId.isBlank()) {
            return OAuth2TokenValidatorResult.failure(SEM_IDENTIFICADOR);
        }
        if (revokedTokenRepository.existsByTokenId(tokenId)) {
            return OAuth2TokenValidatorResult.failure(TOKEN_REVOGADO);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
