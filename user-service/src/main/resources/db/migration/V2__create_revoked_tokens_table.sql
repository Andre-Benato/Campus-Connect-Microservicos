-- RF02: o logout precisa invalidar um token JWT que, por ser stateless, continuaria
-- valido ate expirar. A tabela funciona como denylist: um token so e aceito enquanto
-- o seu identificador (claim "jti") nao estiver registrado aqui.
--
-- expires_at e revoked_at sao DATETIMEOFFSET porque a entidade usa java.time.Instant,
-- que o Hibernate mapeia para TIMESTAMP_WITH_TIMEZONE no SQL Server; DATETIME2 faria o
-- ddl-auto=validate recusar a coluna. create_at (V1) e LocalDateTime, logo DATETIME2.
CREATE TABLE revoked_tokens (
    id         BIGINT         IDENTITY(1,1) PRIMARY KEY,
    token_id   VARCHAR(64)    NOT NULL,
    user_id    BIGINT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at DATETIMEOFFSET NOT NULL,
    revoked_at DATETIMEOFFSET NOT NULL
);

-- Consultado a cada requisicao autenticada; o indice unico tambem torna o logout idempotente.
CREATE UNIQUE INDEX ux_revoked_tokens_token_id ON revoked_tokens (token_id);

-- Usado pela rotina que descarta os registros cujo token ja expirou por conta propria.
CREATE INDEX ix_revoked_tokens_expires_at ON revoked_tokens (expires_at);
