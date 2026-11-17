package com.campusconnect.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Registro de um token JWT invalidado pelo logout (RF02).
 *
 * <p>Como o JWT e stateless, o servidor nao tem como "apagar" um token ja emitido. O logout grava
 * aqui o identificador do token (claim {@code jti}) e toda requisicao autenticada passa a ser
 * recusada enquanto esse identificador estiver na tabela.
 *
 * <p>O campo {@code expiresAt} guarda o momento em que o token expiraria por conta propria: depois
 * disso o registro e descartavel, porque o proprio JWT ja seria recusado por expiracao.
 */
@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Claim {@code jti} do token revogado. */
    @Column(name = "token_id", nullable = false, unique = true, length = 64)
    private String tokenId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at", nullable = false, updatable = false)
    private Instant revokedAt;

    protected RevokedToken() {
        // exigido pelo JPA
    }

    public RevokedToken(String tokenId, Long userId, Instant expiresAt) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void preencherRevokedAt() {
        if (revokedAt == null) {
            revokedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getTokenId() {
        return tokenId;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
