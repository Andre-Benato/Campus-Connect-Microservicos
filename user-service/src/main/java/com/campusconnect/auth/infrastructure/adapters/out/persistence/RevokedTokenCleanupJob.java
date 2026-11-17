package com.campusconnect.auth.infrastructure.adapters.out.persistence;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Descarta periodicamente os tokens revogados que ja passaram da propria expiracao.
 *
 * <p>Depois do {@code exp} do token, o registro na denylist deixa de ter utilidade: o proprio JWT ja
 * seria recusado por expiracao. Sem essa limpeza a tabela cresceria indefinidamente e a consulta
 * feita a cada requisicao autenticada ficaria cada vez mais cara.
 */
@Component
public class RevokedTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RevokedTokenCleanupJob.class);

    private final RevokedTokenRepository revokedTokenRepository;

    public RevokedTokenCleanupJob(RevokedTokenRepository revokedTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Transactional
    @Scheduled(
            initialDelayString = "${campusconnect.security.jwt.cleanup-interval}",
            fixedDelayString = "${campusconnect.security.jwt.cleanup-interval}")
    public void removerTokensExpirados() {
        int removidos = revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
        if (removidos > 0) {
            log.info("Removidos {} tokens revogados que ja haviam expirado", removidos);
        }
    }
}
