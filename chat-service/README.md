# Campus Connect — chat-service

Microservico de **chat 1:1** (REST + STOMP/WebSocket). Persiste no MongoDB e cria conversa automatica ao consumir `RideRequestAccepted` do Azure Service Bus.

| Item | Valor |
|------|-------|
| Porta (host) | **8082** |
| Stack | Java 25, Spring Boot 4.1, MongoDB, WebSocket/STOMP |
| Auth | Valida Bearer via `POST {USER_SERVICE_URL}/api/v1/auth/validate` |
| Swagger | `http://localhost:8082/swagger-ui.html` |
| OpenAPI | `http://localhost:8082/v3/api-docs` |

Arquitetura **Hexagonal** (`domain` / `application` / `infrastructure` + ports de messaging).

## Endpoints REST

Todas as rotas exigem `Authorization: Bearer <token>`.

| Metodo | Endpoint | Descricao |
|--------|----------|-----------|
| `POST` | `/api/v1/messages` | Envia mensagem (`recipientId`, `content`) → `201` |
| `GET` | `/api/v1/conversations` | Lista conversas (ultima mensagem) |
| `GET` | `/api/v1/conversations/{otherUserId}/messages` | Historico 1:1 (`before`, `limit` opcionais) |

Preferir o **gateway** (`:8090`).

## STOMP / WebSocket

| | Via gateway | Direto |
|--|-------------|--------|
| Endpoint | `ws://localhost:8090/ws` | `ws://localhost:8082/ws` |
| CONNECT | header `Authorization: Bearer <token>` | idem |
| Enviar | `/app/chat.send` `{ "recipientId", "content" }` | idem |
| Receber | `/user/queue/messages` | idem |

## Eventos (consumer)

```
[ carona-service ] --publish--> [ Azure Service Bus: ride-request-accepted ] --consume--> [ chat-service ]
```

Sem `AZURE_SERVICEBUS_CONNECTION_STRING`, o consumer nao inicia (dev local ok).

## Variaveis

| Variavel | Uso |
|----------|-----|
| `CHAT_MONGODB_URI` | Compose: `mongodb://mongo:27017/campusconnect_chat` |
| `USER_SERVICE_URL` | Local: `http://localhost:8081` · Compose: `http://user-service:8080` |
| `AZURE_SERVICEBUS_CONNECTION_STRING` | Connection string do namespace |
| `AZURE_SERVICEBUS_QUEUE_RIDE_REQUEST_ACCEPTED` | Default `ride-request-accepted` |

Config completa: [`.env.example`](../.env.example).

## Como executar

```bash
# Via Compose (raiz do repo) — sobe mongo + chat
docker compose up --build chat-service

# Local (Mongo + user-service no ar)
cd chat-service && mvn spring-boot:run
cd chat-service && mvn test
```

Visao geral do monorepo: [`../README.md`](../README.md).
