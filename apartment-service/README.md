# Campus Connect — apartment-service

Microservico de **CRUD de apartamentos**. Guarda apenas `userId` (referencia logica, sem FK entre bancos).

| Item | Valor |
|------|-------|
| Porta (host) | **8080** |
| Stack | Java 25, Spring Boot 4.1, JPA, SQL Server |
| Auth | Valida Bearer via `POST {USER_SERVICE_URL}/api/v1/auth/validate` |
| Swagger | `http://localhost:8080/swagger-ui.html` |
| OpenAPI | `http://localhost:8080/v3/api-docs` |

Arquitetura **Hexagonal** (`domain` / `application` / `infrastructure`).

## Endpoints

Todas as rotas exigem `Authorization: Bearer <token>`.

| Metodo | Endpoint | Descricao |
|--------|----------|-----------|
| `POST` | `/api/v1/apartments` | Cria anuncio |
| `GET` | `/api/v1/apartments` | Lista/filtra (`city`, `neighborhood`, `minRent`, `maxRent`, `bedrooms`, `available`) |
| `GET` | `/api/v1/apartments/my` | Anuncios do usuario autenticado |
| `GET` | `/api/v1/apartments/{id}` | Detalhe |
| `PUT` | `/api/v1/apartments/{id}` | Atualiza |
| `DELETE` | `/api/v1/apartments/{id}` | Remove |

Preferir o **gateway** (`:8090`).

## Variaveis

| Variavel | Uso |
|----------|-----|
| `DB_URL` / `APARTMENT_DB_URL` | JDBC SQL Server (banco proprio) |
| `DB_USERNAME` / `APARTMENT_DB_USERNAME` | Usuario do banco |
| `DB_PASSWORD` / `APARTMENT_DB_PASSWORD` | Senha do banco |
| `USER_SERVICE_URL` | Local: `http://localhost:8081` · Compose: `http://user-service:8080` |

Config completa: [`.env.example`](../.env.example).

## Como executar

```bash
# Via Compose (raiz do repo)
docker compose up --build apartment-service

# Local (user-service precisa estar no ar)
cd apartment-service && mvn spring-boot:run
cd apartment-service && mvn test
```

Visao geral do monorepo: [`../README.md`](../README.md).
