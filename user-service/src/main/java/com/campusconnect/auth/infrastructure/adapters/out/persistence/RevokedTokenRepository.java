package com.campusconnect.auth.infrastructure.adapters.out.persistence;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.campusconnect.auth.domain.RevokedToken;
/** Consultas da denylist de tokens JWT. */
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    /** Consultado a cada requisicao autenticada para decidir se o token ainda vale. */
    boolean existsByTokenId(String tokenId);

    /** Remove os registros cujo token ja expirou e que, portanto, nao precisam mais ser guardados. */
    @Modifying
    @Query("delete from RevokedToken t where t.expiresAt < :limite")
    int deleteByExpiresAtBefore(@Param("limite") Instant limite);
}
