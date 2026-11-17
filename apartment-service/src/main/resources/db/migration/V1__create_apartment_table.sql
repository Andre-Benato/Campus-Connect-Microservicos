CREATE TABLE apartments (
                            id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
                            name                VARCHAR(120) NOT NULL,
                            description         VARCHAR(1000),
                            address             VARCHAR(200) NOT NULL,
                            number              VARCHAR(20) NOT NULL,
                            complement          VARCHAR(100),
                            neighborhood        VARCHAR(100) NOT NULL,
                            city                VARCHAR(100) NOT NULL,
                            state               VARCHAR(2) NOT NULL,
                            cep                 VARCHAR(9) NOT NULL,
                            bedrooms            INT NOT NULL,
                            bathrooms           INT NOT NULL,
                            rent_price          DECIMAL(10,2) NOT NULL,
                            condominium_price   DECIMAL(10,2),
                            available           BIT NOT NULL DEFAULT 1,
                            user_id             BIGINT NOT NULL
);

CREATE INDEX ix_apartments_user_id
    ON apartments (user_id);
