package com.campusconnect.auth.application;

import com.campusconnect.security.JwtConfig;
import com.campusconnect.security.JwtProperties;
import com.campusconnect.user.domain.User;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.auth.domain.RevokedToken;
import com.campusconnect.auth.infrastructure.adapters.out.persistence.RevokedTokenRepository;
/**
 * Emissao e revogacao dos tokens de acesso (RF02).
 *
 * <p>A verificacao do token na entrada nao passa por aqui: ela fica a cargo do decoder configurado
 * em {@link com.campusconnect.security.JwtConfig}, que roda no filtro de Bearer Token do Spring
 * Security antes de qualquer controller.
 */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final RevokedTokenRepository revokedTokenRepository;

    public JwtService(
            JwtEncoder jwtEncoder,
            JwtProperties properties,
            RevokedTokenRepository revokedTokenRepository) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    /** Token recem-emitido junto com os dados de expiracao devolvidos ao cliente. */
    public record TokenEmitido(String valor, Instant expiraEm, Duration validade) {}

    /**
     * Emite um token para o usuario informado.
     *
     * <p>O id do usuario vai na claim {@code sub} (e o que identifica o dono da sessao nas demais
     * requisicoes) e cada token recebe um {@code jti} unico, que e o que permite revoga-lo no
     * logout.
     */
    public TokenEmitido emitir(User user) {
        Instant agora = Instant.now();
        Duration validade = properties.getExpiration();
        Instant expiraEm = agora.plus(validade);

        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(properties.getIssuer())
                        .issuedAt(agora)
                        .expiresAt(expiraEm)
                        .subject(String.valueOf(user.getId()))
                        .id(UUID.randomUUID().toString())
                        .claim("email", user.getEmail())
                        .claim("name", user.getName())
                        .build();

        JwsHeader header = JwsHeader.with(JwtConfig.ALGORITMO).build();
        String valor = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new TokenEmitido(valor, expiraEm, validade);
    }

    /**
     * Registra o token na denylist. A operacao e idempotente: repetir o logout com o mesmo token nao
     * gera erro nem duplica o registro.
     */
    @Transactional
    public void revogar(Jwt token) {
        String tokenId = token.getId();
        if (revokedTokenRepository.existsByTokenId(tokenId)) {
            return;
        }

        Instant expiraEm = token.getExpiresAt();
        if (expiraEm == null) {
            // Defensivo: todo token emitido aqui tem "exp". Sem essa informacao, guardamos o registro
            // pelo tempo de vida configurado para que a limpeza automatica ainda consiga descarta-lo.
            expiraEm = Instant.now().plus(properties.getExpiration());
        }

        revokedTokenRepository.save(
                new RevokedToken(tokenId, Long.valueOf(token.getSubject()), expiraEm));
    }
}
