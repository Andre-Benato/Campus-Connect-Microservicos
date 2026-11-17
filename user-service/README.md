# Campus Connect — user-service

Microservico de **usuarios e autenticacao JWT**.

| Item | Valor |
|------|-------|
| Porta (host) | **8081** |
| Stack | Java 25, Spring Boot 4.1, JPA, SQL Server |
| Swagger | `http://localhost:8081/swagger-ui.html` |
| OpenAPI | `http://localhost:8081/v3/api-docs` |

Arquitetura **Hexagonal** (`domain` / `application` / `infrastructure`). Pacotes: `user` (CRUD) e `auth` (login, validate, logout).

## Endpoints

| Metodo | Endpoint | Descricao |
|--------|----------|-----------|
| `POST` | `/api/v1/users` | Cadastro |
| `GET` | `/api/v1/users` | Lista usuarios |
| `GET` | `/api/v1/users/{id}` | Detalhe |
| `PUT` | `/api/v1/users/{id}` | Atualiza |
| `DELETE` | `/api/v1/users/{id}` | Remove |
| `POST` | `/api/v1/auth/login` | Login → `accessToken` |
| `POST` | `/api/v1/auth/validate` | Valida Bearer (usado pelos outros servicos) |
| `POST` | `/api/v1/auth/logout` | Revoga token |

Preferir o **gateway** (`:8090`) em vez do acesso direto.

## Variaveis

| Variavel | Uso |
|----------|-----|
| `DB_URL` / `USER_DB_URL` | JDBC SQL Server (banco proprio) |
| `DB_USERNAME` / `USER_DB_USERNAME` | Usuario do banco |
| `DB_PASSWORD` / `USER_DB_PASSWORD` | Senha do banco |
| `CAMPUSCONNECT_PASSWORD_SECRET` | Pepper do BCrypt |
| `CAMPUSCONNECT_JWT_SECRET` | Secret HS256 (≥ 32 chars) |
| `CAMPUSCONNECT_JWT_EXPIRATION` | Ex.: `5h` |
| `CAMPUSCONNECT_JWT_ISSUER` | Ex.: `campus-connect` |

Config completa: [`.env.example`](../.env.example).

## Como executar

```bash
# Via Compose (raiz do repo)
docker compose up --build user-service

# Local
cd user-service && mvn spring-boot:run
cd user-service && mvn test
```

Visao geral do monorepo: [`../README.md`](../README.md).
