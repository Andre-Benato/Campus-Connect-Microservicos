package com.campusconnect.chat.infrastructure.adapters.in.web;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusconnect.chat.infrastructure.adapters.in.web.dto.MessageResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import tools.jackson.databind.ObjectMapper;

import com.campusconnect.chat.infrastructure.adapters.out.persistence.MessageRepository;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class ChatIntegrationTest {

    private static final String TOKEN_A = "token-usuario-a";
    private static final String TOKEN_B = "token-usuario-b";
    private static final String TOKEN_C = "token-usuario-c";
    private static final long USER_A = 1L;
    private static final long USER_B = 2L;
    private static final long USER_C = 3L;

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    static final WireMockServer userService;

    static {
        userService = new WireMockServer(wireMockConfig().dynamicPort());
        userService.start();
    }

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageRepository messageRepository;

    @AfterAll
    static void pararWireMock() {
        userService.stop();
    }

    @DynamicPropertySource
    static void apontarUserService(DynamicPropertyRegistry registry) {
        registry.add("campusconnect.user-service.base-url", userService::baseUrl);
        registry.add("campusconnect.servicebus.connection-string", () -> "");
    }

    @BeforeEach
    void limpar() {
        messageRepository.deleteAll();
        userService.resetAll();
        stubValidate(TOKEN_A, USER_A, "a@campusconnect.dev", "Usuario A");
        stubValidate(TOKEN_B, USER_B, "b@campusconnect.dev", "Usuario B");
        stubValidate(TOKEN_C, USER_C, "c@campusconnect.dev", "Usuario C");
    }

    @Test
    void deveResponder401QuandoNaoHaToken() throws Exception {
        mockMvc
                .perform(get("/api/v1/conversations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Nao autenticado"));
    }

    @Test
    void deveResponder400QuandoEnviaMensagemParaSiMesmo() throws Exception {
        mockMvc
                .perform(
                        post("/api/v1/messages")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_A)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of("recipientId", USER_A, "content", "Oi"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisicao invalida"));
    }

    @Test
    void deveCriarMensagemQuandoPostValido() throws Exception {
        mockMvc
                .perform(
                        post("/api/v1/messages")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_A)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                Map.of("recipientId", USER_B, "content", "Ola"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conversationId").value("1_2"))
                .andExpect(jsonPath("$.senderId").value(USER_A))
                .andExpect(jsonPath("$.recipientId").value(USER_B))
                .andExpect(jsonPath("$.content").value("Ola"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        assertThat(messageRepository.count()).isEqualTo(1);
    }

    @Test
    void historicoDeveRetornarApenasMensagensDaConversaEntreOsDoisUsuarios() throws Exception {
        enviarMensagem(TOKEN_A, USER_B, "A para B");
        enviarMensagem(TOKEN_B, USER_A, "B para A");
        enviarMensagem(TOKEN_A, USER_C, "A para C");

        mockMvc
                .perform(
                        get("/api/v1/conversations/{otherUserId}/messages", USER_B)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("A para B"))
                .andExpect(jsonPath("$[1].content").value("B para A"))
                .andExpect(jsonPath("$[0].conversationId").value("1_2"))
                .andExpect(jsonPath("$[1].conversationId").value("1_2"));
    }

    @Test
    void listarConversasDeveAparecerAposAoMenosUmaMensagem() throws Exception {
        mockMvc
                .perform(
                        get("/api/v1/conversations")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        enviarMensagem(TOKEN_A, USER_B, "Primeira");

        mockMvc
                .perform(
                        get("/api/v1/conversations")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].conversationId").value("1_2"))
                .andExpect(jsonPath("$[0].otherUserId").value(USER_B))
                .andExpect(jsonPath("$[0].lastMessage.content").value("Primeira"))
                .andExpect(jsonPath("$[0].lastMessage.senderId").value(USER_A));
    }

    @Test
    void deveEditarMensagemQuandoPutValidoDoRemetente() throws Exception {
        MessageResponse criada = criarMensagem(TOKEN_A, USER_B, "Ola");

        mockMvc
                .perform(
                        put("/api/v1/messages/{id}", criada.id())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_A)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("content", "Ola editado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(criada.id()))
                .andExpect(jsonPath("$.content").value("Ola editado"))
                .andExpect(jsonPath("$.editedAt").isNotEmpty())
                .andExpect(jsonPath("$.deleted").value(false));
    }

    @Test
    void deveResponder403QuandoOutroUsuarioTentaEditar() throws Exception {
        MessageResponse criada = criarMensagem(TOKEN_A, USER_B, "Ola");

        mockMvc
                .perform(
                        put("/api/v1/messages/{id}", criada.id())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_B)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("content", "hack"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso negado"));
    }

    @Test
    void deveResponder404QuandoEditaMensagemInexistente() throws Exception {
        mockMvc
                .perform(
                        put("/api/v1/messages/{id}", "000000000000000000000000")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_A)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("content", "oi"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso nao encontrado"));
    }

    @Test
    void deveExcluirMensagemQuandoDeleteDoRemetente() throws Exception {
        MessageResponse criada = criarMensagem(TOKEN_A, USER_B, "Ola");

        mockMvc
                .perform(
                        delete("/api/v1/messages/{id}", criada.id())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_A))
                .andExpect(status().isNoContent());

        assertThat(messageRepository.findById(criada.id())).isEmpty();
    }

    @Test
    void historicoNaoDeveIncluirMensagemExcluida() throws Exception {
        criarMensagem(TOKEN_A, USER_B, "fica");
        MessageResponse excluida = criarMensagem(TOKEN_A, USER_B, "sai");

        mockMvc
                .perform(
                        delete("/api/v1/messages/{id}", excluida.id())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_A))
                .andExpect(status().isNoContent());

        mockMvc
                .perform(
                        get("/api/v1/conversations/{otherUserId}/messages", USER_B)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("fica"));
    }

    @Test
    void deveResponder403QuandoOutroUsuarioTentaExcluir() throws Exception {
        MessageResponse criada = criarMensagem(TOKEN_A, USER_B, "Ola");

        mockMvc
                .perform(
                        delete("/api/v1/messages/{id}", criada.id())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_B))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acesso negado"));

        assertThat(messageRepository.findById(criada.id())).isPresent();
    }

    @Test
    void stompConnectSemBearerDeveSerRecusado() {
        WebSocketStompClient client = novoStompClient();

        assertThatThrownBy(
                        () ->
                                conectarStomp(client, new StompHeaders())
                                        .get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(Exception.class);
    }

    @Test
    void destinatarioConectadoViaStompDeveReceberMensagemEmTempoReal() throws Exception {
        WebSocketStompClient client = novoStompClient();
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_B);

        StompSession session = conectarStomp(client, connectHeaders).get(5, TimeUnit.SECONDS);

        CompletableFuture<MessageResponse> recebida = new CompletableFuture<>();
        session.subscribe(
                "/user/queue/messages",
                new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return MessageResponse.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        recebida.complete((MessageResponse) payload);
                    }
                });

        enviarMensagem(TOKEN_A, USER_B, "Push em tempo real");

        MessageResponse mensagem = recebida.get(5, TimeUnit.SECONDS);
        assertThat(mensagem.content()).isEqualTo("Push em tempo real");
        assertThat(mensagem.senderId()).isEqualTo(USER_A);
        assertThat(mensagem.recipientId()).isEqualTo(USER_B);
        assertThat(mensagem.conversationId()).isEqualTo("1_2");

        session.disconnect();
    }

    private void enviarMensagem(String token, long recipientId, String content) throws Exception {
        criarMensagem(token, recipientId, content);
    }

    private MessageResponse criarMensagem(String token, long recipientId, String content)
            throws Exception {
        String body =
                mockMvc
                        .perform(
                                post("/api/v1/messages")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        Map.of(
                                                                "recipientId",
                                                                recipientId,
                                                                "content",
                                                                content))))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return objectMapper.readValue(body, MessageResponse.class);
    }

    private static WebSocketStompClient novoStompClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        return client;
    }

    private CompletableFuture<StompSession> conectarStomp(
            WebSocketStompClient client, StompHeaders connectHeaders) {
        return client.connectAsync(
                URI.create("ws://localhost:" + port + "/ws"),
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {});
    }

    private static void stubValidate(String token, long userId, String email, String name) {
        userService.stubFor(
                WireMock.post(urlEqualTo("/api/v1/auth/validate"))
                        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + token))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                HttpHeaders.CONTENT_TYPE,
                                                MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(
                                                """
                                                {"userId":%d,"email":"%s","name":"%s"}
                                                """
                                                        .formatted(userId, email, name))));
    }
}
