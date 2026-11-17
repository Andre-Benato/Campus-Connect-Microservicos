package com.campusconnect.ride.infrastructure.adapters.in.web;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.campusconnect.ride.domain.Ride;
import com.campusconnect.ride.infrastructure.adapters.out.persistence.RideRepository;
import com.campusconnect.ride.infrastructure.adapters.out.persistence.RideRequestRepository;
import com.campusconnect.ride.domain.RideStatus;
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RideIntegrationTest {

    private static final String DRIVER_TOKEN = "token-motorista";
    private static final String PASSENGER_TOKEN = "token-passageiro";
    private static final long DRIVER_ID = 10L;
    private static final long PASSENGER_ID = 20L;

    @Container
    @ServiceConnection
    static MSSQLServerContainer sqlServer =
            new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    static final WireMockServer userService;

    static {
        userService = new WireMockServer(wireMockConfig().dynamicPort());
        userService.start();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private RideRequestRepository rideRequestRepository;

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
        rideRequestRepository.deleteAll();
        rideRepository.deleteAll();
        userService.resetAll();
        stubValidate(DRIVER_TOKEN, DRIVER_ID, "motorista@campusconnect.dev", "Motorista");
        stubValidate(PASSENGER_TOKEN, PASSENGER_ID, "passageiro@campusconnect.dev", "Passageiro");
    }

    @Test
    void deveCriarOfertaQuandoUserServiceValidaOToken() throws Exception {
        mockMvc
                .perform(
                        post("/api/v1/rides")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + DRIVER_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(ofertaValida())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin").value("Centro"))
                .andExpect(jsonPath("$.destination").value("Campus"))
                .andExpect(jsonPath("$.driverUserId").value(DRIVER_ID))
                .andExpect(jsonPath("$.availableSeats").value(3))
                .andExpect(jsonPath("$.status").value("OPEN"));

        userService.verify(
                postRequestedFor(urlEqualTo("/api/v1/auth/validate"))
                        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + DRIVER_TOKEN)));
    }

    @Test
    void deveResponder401QuandoNaoHaToken() throws Exception {
        mockMvc
                .perform(get("/api/v1/rides"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Nao autenticado"));
    }

    @Test
    void deveListarEFiltrarOfertasDisponiveis() throws Exception {
        salvarOferta("Centro", "Campus", 2, RideStatus.OPEN);
        salvarOferta("Batel", "Campus", 0, RideStatus.OPEN);
        salvarOferta("Centro", "Aeroporto", 1, RideStatus.CANCELLED);

        mockMvc
                .perform(
                        get("/api/v1/rides")
                                .param("origin", "centro")
                                .param("onlyAvailable", "true")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + PASSENGER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].destination").value("Campus"))
                .andExpect(jsonPath("$[0].availableSeats").value(2));
    }

    @Test
    void deveSolicitarAceitarEDecrementarAssentos() throws Exception {
        Long rideId = criarOfertaComoMotorista();

        MvcResult solicitacao =
                mockMvc
                        .perform(
                                post("/api/v1/rides/" + rideId + "/requests")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + PASSENGER_TOKEN)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        Map.of("seatsRequested", 1, "message", "Posso ir?"))))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("PENDING"))
                        .andExpect(jsonPath("$.passengerUserId").value(PASSENGER_ID))
                        .andReturn();

        long requestId = objectMapper.readTree(solicitacao.getResponse().getContentAsString()).get("id").asLong();

        mockMvc
                .perform(
                        post("/api/v1/rides/" + rideId + "/requests/" + requestId + "/accept")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + DRIVER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc
                .perform(
                        get("/api/v1/rides/" + rideId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + DRIVER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSeats").value(2));
    }

    @Test
    void deveRecusarAutoSolicitacaoDoMotorista() throws Exception {
        Long rideId = criarOfertaComoMotorista();

        mockMvc
                .perform(
                        post("/api/v1/rides/" + rideId + "/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + DRIVER_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("seatsRequested", 1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Regra de negocio violada"));
    }

    @Test
    void deveImpedirSegundaSolicitacaoAtivaDoMesmoPassageiro() throws Exception {
        Long rideId = criarOfertaComoMotorista();

        mockMvc
                .perform(
                        post("/api/v1/rides/" + rideId + "/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + PASSENGER_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("seatsRequested", 1))))
                .andExpect(status().isCreated());

        mockMvc
                .perform(
                        post("/api/v1/rides/" + rideId + "/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + PASSENGER_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("seatsRequested", 1))))
                .andExpect(status().isConflict());
    }

    @Test
    void deveCancelarSolicitacaoAceitaERestaurarAssentos() throws Exception {
        Long rideId = criarOfertaComoMotorista();

        MvcResult solicitacao =
                mockMvc
                        .perform(
                                post("/api/v1/rides/" + rideId + "/requests")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + PASSENGER_TOKEN)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(Map.of("seatsRequested", 2))))
                        .andExpect(status().isCreated())
                        .andReturn();

        long requestId = objectMapper.readTree(solicitacao.getResponse().getContentAsString()).get("id").asLong();

        mockMvc
                .perform(
                        post("/api/v1/rides/" + rideId + "/requests/" + requestId + "/accept")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + DRIVER_TOKEN))
                .andExpect(status().isOk());

        mockMvc
                .perform(
                        post("/api/v1/rides/" + rideId + "/requests/" + requestId + "/cancel")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + PASSENGER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc
                .perform(
                        get("/api/v1/rides/" + rideId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + DRIVER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSeats").value(3));
    }

    @Test
    void deveListarSolicitacoesApenasParaMotorista() throws Exception {
        Long rideId = criarOfertaComoMotorista();

        mockMvc
                .perform(
                        post("/api/v1/rides/" + rideId + "/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + PASSENGER_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("seatsRequested", 1))))
                .andExpect(status().isCreated());

        mockMvc
                .perform(
                        get("/api/v1/rides/" + rideId + "/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + DRIVER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc
                .perform(
                        get("/api/v1/rides/" + rideId + "/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + PASSENGER_TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveAtualizarOfertaSomenteDoDono() throws Exception {
        Long rideId = criarOfertaComoMotorista();

        Map<String, Object> update = ofertaValida();
        update.put("origin", "Batel");
        update.put("totalSeats", 4);

        mockMvc
                .perform(
                        put("/api/v1/rides/" + rideId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + DRIVER_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origin").value("Batel"))
                .andExpect(jsonPath("$.totalSeats").value(4))
                .andExpect(jsonPath("$.availableSeats").value(4));

        mockMvc
                .perform(
                        put("/api/v1/rides/" + rideId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + PASSENGER_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveCancelarOfertaECancelarSolicitacoesAtivas() throws Exception {
        Long rideId = criarOfertaComoMotorista();

        mockMvc
                .perform(
                        post("/api/v1/rides/" + rideId + "/requests")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + PASSENGER_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("seatsRequested", 1))))
                .andExpect(status().isCreated());

        mockMvc
                .perform(
                        post("/api/v1/rides/" + rideId + "/cancel")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + DRIVER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc
                .perform(
                        get("/api/v1/ride-requests/my")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + PASSENGER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CANCELLED"));
    }

    private Long criarOfertaComoMotorista() throws Exception {
        MvcResult result =
                mockMvc
                        .perform(
                                post("/api/v1/rides")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + DRIVER_TOKEN)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(ofertaValida())))
                        .andExpect(status().isCreated())
                        .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private void stubValidate(String token, long userId, String email, String name) {
        userService.stubFor(
                WireMock.post(urlEqualTo("/api/v1/auth/validate"))
                        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + token))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(
                                                """
                                                {"userId":%d,"email":"%s","name":"%s"}
                                                """
                                                        .formatted(userId, email, name))));
    }

    private Map<String, Object> ofertaValida() {
        return new java.util.LinkedHashMap<>(
                Map.of(
                        "origin",
                        "Centro",
                        "destination",
                        "Campus",
                        "departureAt",
                        LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS).toString(),
                        "totalSeats",
                        3,
                        "pricePerSeat",
                        new BigDecimal("10.00"),
                        "notes",
                        "Saida perto da praca"));
    }

    private void salvarOferta(String origin, String destination, int availableSeats, RideStatus status) {
        Ride ride =
                new Ride(
                        DRIVER_ID,
                        origin,
                        destination,
                        LocalDateTime.now().plusDays(1),
                        Math.max(availableSeats, 1),
                        new BigDecimal("8.00"),
                        null);
        ride.setAvailableSeats(availableSeats);
        ride.setStatus(status);
        if (availableSeats == 0) {
            ride.setTotalSeats(2);
            ride.setAvailableSeats(0);
        }
        rideRepository.save(ride);
    }
}
