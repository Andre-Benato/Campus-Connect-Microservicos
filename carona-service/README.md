# Campus Connect — carona-service

Microservico de **ofertas e solicitacoes de carona** (RF09/RF10). Ao aceitar uma solicitacao, publica `RideRequestAccepted` no Azure Service Bus para o chat-service.

| Item | Valor |
|------|-------|
| Porta (host) | **8083** |
| Stack | Java 25, Spring Boot 4.1, JPA, SQL Server |
| Auth | Valida Bearer via `POST {USER_SERVICE_URL}/api/v1/auth/validate` |
| Swagger | `http://localhost:8083/swagger-ui.html` |
| OpenAPI | `http://localhost:8083/v3/api-docs` |

Arquitetura **Hexagonal** (`domain` / `application` / `infrastructure` + porta `RideRequestAcceptedPublisher`).

## Endpoints

Todas as rotas exigem `Authorization: Bearer <token>`. Preferir o **gateway** (`:8090`).

| Metodo | Endpoint | Descricao |
|--------|----------|-----------|
| `POST` | `/api/v1/rides` | Cria oferta |
| `GET` | `/api/v1/rides` | Lista/filtra (`origin`, `destination`, `departureFrom`, `departureTo`, `onlyAvailable`) |
| `GET` | `/api/v1/rides/my` | Ofertas do motorista autenticado |
| `GET` | `/api/v1/rides/{id}` | Detalhe |
| `PUT` | `/api/v1/rides/{id}` | Atualiza (`OPEN`, so motorista) |
| `POST` | `/api/v1/rides/{id}/cancel` | Cancela oferta |
| `POST` | `/api/v1/rides/{id}/complete` | Encerra oferta |
| `POST` | `/api/v1/rides/{id}/requests` | Solicita vaga |
| `GET` | `/api/v1/rides/{id}/requests` | Lista solicitacoes (so motorista) |
| `POST` | `/api/v1/rides/{id}/requests/{requestId}/accept` | Aceita (publica evento) |
| `POST` | `/api/v1/rides/{id}/requests/{requestId}/reject` | Recusa |
| `POST` | `/api/v1/rides/{id}/requests/{requestId}/cancel` | Cancela solicitacao |
| `GET` | `/api/v1/ride-requests/my` | Solicitacoes do passageiro |

## Eventos (publisher)

```
[ carona-service ] --publish--> [ Azure Service Bus: ride-request-accepted ] --consume--> [ chat-service ]
```

Sem connection string: o publisher apenas loga o evento (dev local ok).

## Variaveis

| Variavel | Uso |
|----------|-----|
| `DB_URL` / `CARONA_DB_URL` | JDBC SQL Server (banco proprio) |
| `DB_USERNAME` / `CARONA_DB_USERNAME` | Usuario do banco |
| `DB_PASSWORD` / `CARONA_DB_PASSWORD` | Senha do banco |
| `USER_SERVICE_URL` | Local: `http://localhost:8081` · Compose: `http://user-service:8080` |
| `AZURE_SERVICEBUS_CONNECTION_STRING` | Connection string do namespace |
| `AZURE_SERVICEBUS_QUEUE_RIDE_REQUEST_ACCEPTED` | Default `ride-request-accepted` |

Config completa: [`.env.example`](../.env.example).

## Como executar

```bash
# Via Compose (raiz do repo)
docker compose up --build carona-service

# Local (user-service no ar)
cd carona-service && mvn spring-boot:run
cd carona-service && mvn test
```

Visao geral do monorepo: [`../README.md`](../README.md).
