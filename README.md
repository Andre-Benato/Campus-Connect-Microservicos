## Microservicos

| Servico | Pasta | Porta (host) | Responsabilidade |
|---------|-------|--------------|------------------|
| **gateway-service** | [`gateway-service/`](gateway-service/README.md) | `8090` | **Ponto unico de entrada** (API Gateway + BFF): proxy, CORS, rastreio e agregacao |
| user-service | [`user-service/`](user-service/README.md) | `8081` | Cadastro de usuarios, login/logout JWT, `POST /api/v1/auth/validate` |
| apartment-service | [`apartment-service/`](apartment-service/README.md) | `8080` | CRUD de apartamentos; valida o Bearer token chamando o user-service |
| chat-service | [`chat-service/`](chat-service/README.md) | `8082` | Chat 1:1 (REST + STOMP/WebSocket); persiste no MongoDB; valida Bearer via user-service; consome `RideRequestAccepted` do Azure Service Bus |
| carona-service | [`carona-service/`](carona-service/README.md) | `8083` | Ofertas e solicitacoes de carona (RF09/RF10); valida Bearer via user-service; publica `RideRequestAccepted` no Azure Service Bus |

### Swagger / OpenAPI (cada microserviço)

| Serviço | Swagger UI | OpenAPI JSON |
|---------|------------|--------------|
| gateway-service | `http://localhost:8090/swagger-ui.html` | `http://localhost:8090/v3/api-docs` |
| user-service | `http://localhost:8081/swagger-ui.html` | `http://localhost:8081/v3/api-docs` |
| apartment-service | `http://localhost:8080/swagger-ui.html` | `http://localhost:8080/v3/api-docs` |
| chat-service | `http://localhost:8082/swagger-ui.html` | `http://localhost:8082/v3/api-docs` |
| carona-service | `http://localhost:8083/swagger-ui.html` | `http://localhost:8083/v3/api-docs` |

Headers injetados nas respostas roteadas: `X-Gateway-Processed-By`, `X-Gateway-Route`, `X-Gateway-Execution-Time-Ms`, `X-Correlation-Id`.

O BFF propaga o header `Authorization` quando o cliente o envia (endpoints de listagem exigem JWT nos microservicos).

## Arquitetura de codigo: Hexagonal (Ports & Adapters)

Os microservicos de dominio e o BFF do gateway seguem **Arquitetura Hexagonal (Ports & Adapters)**:

```
com.campusconnect.<dominio>
  ├── domain/                 # entidades e regras de dominio
  ├── application/            # services / use cases
  │     └── ports/            # portas de saida (ex.: RideRequestAcceptedPublisher)
  └── infrastructure/
        ├── adapters/in/web/          # controllers + DTOs HTTP
        ├── adapters/out/persistence/ # Spring Data repositories
        ├── adapters/out/messaging/   # Azure Service Bus
        └── adapters/out/http/        # clientes REST (validacao JWT)
```

Pacotes transversais (`security`, `config`, `common`) permanecem na raiz do modulo. Detalhamento Arc42: [`MeuSistema.md` §8.5](MeuSistema.md).

## Configuracao

```bash
cp .env.example .env
# Ajuste USER_DB_*, APARTMENT_DB_*, CARONA_DB_*, CHAT_*, secrets JWT/password, USER_SERVICE_URL e AZURE_SERVICEBUS_*
```

- Local (servicos fora do Docker): `USER_SERVICE_URL=http://localhost:8081`
- Compose: `USER_SERVICE_URL=http://user-service:8080` (default no `.env.example`)
- Chat (Compose): `CHAT_MONGODB_URI=mongodb://mongo:27017/campusconnect_chat`
- Gateway local: `USER_SERVICE_URL=http://localhost:8081`, `APARTMENT_SERVICE_URL=http://localhost:8080`, `CHAT_SERVICE_URL=http://localhost:8082`, `CARONA_SERVICE_URL=http://localhost:8083`
- Service Bus: preencha `AZURE_SERVICEBUS_CONNECTION_STRING` para ativar o fluxo assincrono carona → chat[text](../CampusConnect/MeuSistema.md)

## Subir com Docker Compose

```bash
docker compose up --build
```

- **gateway-service:** `http://localhost:8090` (ponto unico recomendado)
- user-service: `http://localhost:8081`
- apartment-service: `http://localhost:8080`
- chat-service: `http://localhost:8082`
- carona-service: `http://localhost:8083`
- mongo: interno ao Compose (sem porta publicada por padrao)

## Fluxo tipico via Gateway

1. `POST http://localhost:8090/api/v1/users` — cadastro
2. `POST http://localhost:8090/api/v1/auth/login` — recebe `accessToken`
3. `POST http://localhost:8090/api/v1/apartments` com `Authorization: Bearer <accessToken>`
4. `GET http://localhost:8090/api/bff/dashboard` com o mesmo Bearer
5. `GET http://localhost:8090/api/bff/status` — topologia (sem auth)
6. `GET http://localhost:8090/actuator/gateway/routes` — inspecionar rotas

Em toda requisicao autenticada, apartment-service, chat-service e carona-service chamam `POST {USER_SERVICE_URL}/api/v1/auth/validate` com o mesmo Bearer.

## Caronas (REST)

Todas as rotas exigem `Authorization: Bearer <token>`. Preferir o gateway (`:8090`).

| Metodo | Endpoint | Descricao |
|--------|----------|-----------|
| `POST` | `/api/v1/rides` | Cria oferta de carona |
| `GET` | `/api/v1/rides` | Lista/filtra ofertas (`origin`, `destination`, `departureFrom`, `departureTo`, `onlyAvailable`) |
| `GET` | `/api/v1/rides/my` | Ofertas do motorista autenticado |
| `GET` | `/api/v1/rides/{id}` | Detalhe da oferta |
| `PUT` | `/api/v1/rides/{id}` | Atualiza oferta (`OPEN`, so motorista) |
| `POST` | `/api/v1/rides/{id}/cancel` | Cancela oferta |
| `POST` | `/api/v1/rides/{id}/complete` | Encerra oferta |
| `POST` | `/api/v1/rides/{id}/requests` | Solicita vaga |
| `GET` | `/api/v1/rides/{id}/requests` | Lista solicitacoes (so motorista) |
| `POST` | `/api/v1/rides/{id}/requests/{requestId}/accept` | Aceita solicitacao |
| `POST` | `/api/v1/rides/{id}/requests/{requestId}/reject` | Recusa solicitacao |
| `POST` | `/api/v1/rides/{id}/requests/{requestId}/cancel` | Cancela solicitacao |
| `GET` | `/api/v1/ride-requests/my` | Solicitacoes do passageiro autenticado |

### Fluxo minimo de teste (dois usuarios)

1. Cadastre e faca login de dois usuarios via gateway; guarde `accessToken` A (motorista) e B (passageiro).
2. Com o token A: `POST http://localhost:8090/api/v1/rides` body `{"origin":"Centro","destination":"Campus","departureAt":"<ISO futuro>","totalSeats":3,"pricePerSeat":10.00}`.
3. Com o token B: `POST http://localhost:8090/api/v1/rides/{id}/requests` body `{"seatsRequested":1,"message":"Posso ir?"}`.
4. Com o token A: `POST http://localhost:8090/api/v1/rides/{id}/requests/{requestId}/accept`.
5. Com o token A: `GET http://localhost:8090/api/v1/rides/{id}` — `availableSeats` deve ter decrementado.
6. (Com Service Bus configurado) Com o token B: `GET http://localhost:8090/api/v1/conversations` — deve aparecer conversa com mensagem automatica do motorista.

## Chat (REST + STOMP)

Todas as rotas REST exigem `Authorization: Bearer <token>`. Preferir o gateway (`:8090`); acesso direto ao chat (`:8082`) continua disponivel.

| Metodo | Endpoint | Descricao |
|--------|----------|-----------|
| `POST` | `/api/v1/messages` | Envia mensagem (`recipientId`, `content`) → `201` |
| `GET` | `/api/v1/conversations` | Lista conversas do autenticado (ultima mensagem) |
| `GET` | `/api/v1/conversations/{otherUserId}/messages` | Historico 1:1 (`before`, `limit` opcionais) |

Tempo real (STOMP sobre WebSocket):

- Via gateway: `ws://localhost:8090/ws`
- Direto no chat: `ws://localhost:8082/ws`
- CONNECT com header nativo `Authorization: Bearer <token>`
- Cliente envia: `/app/chat.send` com `{ "recipientId", "content" }`
- Servidor publica: `/user/queue/messages`

### Fluxo minimo de teste (dois usuarios)

1. Cadastre e faça login de dois usuarios via gateway; guarde `accessToken` A e B.
2. Com o token A: `POST http://localhost:8090/api/v1/messages` body `{"recipientId": <idB>, "content": "Ola"}`.
3. Com o token B: `GET http://localhost:8090/api/v1/conversations/{idA}/messages` — deve listar a mensagem.
4. Com o token A ou B: `GET http://localhost:8090/api/v1/conversations` — deve mostrar a conversa com a ultima mensagem.
5. (Opcional) Conecte B via STOMP em `/ws`, inscreva em `/user/queue/messages`, envie outra mensagem com A (REST ou `/app/chat.send`) e confira o push em tempo real.

## Desenvolvimento local (Maven)

Requer Java 25+ para user/apartment/chat/carona; Java 21+ para o gateway.

```bash
cd user-service && mvn spring-boot:run
cd apartment-service && mvn spring-boot:run
cd chat-service && mvn spring-boot:run
cd carona-service && mvn spring-boot:run
cd gateway-service && mvn spring-boot:run
```

Testes (SQL Server / Mongo via Testcontainers nos servicos de dominio):

```bash
cd user-service && mvn test
cd apartment-service && mvn test
cd chat-service && mvn test
cd carona-service && mvn test
cd gateway-service && mvn test
```
