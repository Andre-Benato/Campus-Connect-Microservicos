-- RF01: cadastro de estudantes.
-- "users" no plural porque "user" e palavra reservada em varios bancos; o nome e
-- fixado pela anotacao @Table da entidade User.
CREATE TABLE users (
    id        BIGINT       IDENTITY(1,1) PRIMARY KEY,
    name      VARCHAR(120) NOT NULL,
    email     VARCHAR(180) NOT NULL,
    password  VARCHAR(100) NOT NULL,
    create_at DATETIME2    NOT NULL
);

-- O e-mail identifica o usuario no login (RF02), portanto precisa ser unico.
CREATE UNIQUE INDEX ux_users_email ON users (email);
