package com.campusconnect.apartment.infrastructure.adapters.in.web;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.math.BigDecimal;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import tools.jackson.databind.ObjectMapper;

import com.campusconnect.apartment.domain.Apartment;
import com.campusconnect.apartment.infrastructure.adapters.out.persistence.ApartmentRepository;
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApartmentIntegrationTest {

    private static final String TOKEN = "token-de-teste";
    private static final long USER_ID = 42L;

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
    private ApartmentRepository apartmentRepository;

    @AfterAll
    static void pararWireMock() {
        userService.stop();
    }

    @DynamicPropertySource
    static void apontarUserService(DynamicPropertyRegistry registry) {
        registry.add("campusconnect.user-service.base-url", userService::baseUrl);
    }

    @BeforeEach
    void limpar() {
        apartmentRepository.deleteAll();
        userService.resetAll();
    }

    @Test
    void deveCriarApartamentoQuandoUserServiceValidaOToken() throws Exception {
        stubValidateOk();

        mockMvc
                .perform(
                        post("/api/v1/apartments")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(apartamentoValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Kitnet Centro"))
                .andExpect(jsonPath("$.userId").value(USER_ID));

        userService.verify(
                postRequestedFor(urlEqualTo("/api/v1/auth/validate"))
                        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + TOKEN)));
    }

    @Test
    void deveResponder401QuandoUserServiceRejeitaOToken() throws Exception {
        userService.stubFor(
                WireMock.post(urlEqualTo("/api/v1/auth/validate"))
                        .willReturn(aResponse().withStatus(401)));

        mockMvc
                .perform(
                        get("/api/v1/apartments")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Nao autenticado"));
    }

    @Test
    void deveResponder401QuandoNaoHaToken() throws Exception {
        mockMvc
                .perform(get("/api/v1/apartments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Nao autenticado"));
    }

    @Test
    void deveListarTodosQuandoSemFiltros() throws Exception {
        stubValidateOk();
        salvarApartamento("Kitnet Centro", "Curitiba", "Centro", 1, "1200.00", true);
        salvarApartamento("Apto Batel", "Curitiba", "Batel", 2, "2500.00", true);

        mockMvc
                .perform(get("/api/v1/apartments").header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void deveFiltrarPorCidade() throws Exception {
        stubValidateOk();
        salvarApartamento("Kitnet Centro", "Curitiba", "Centro", 1, "1200.00", true);
        salvarApartamento("Apto Centro", "Sao Paulo", "Centro", 1, "1800.00", true);

        mockMvc
                .perform(
                        get("/api/v1/apartments")
                                .param("city", "curitiba")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].city").value("Curitiba"));
    }

    @Test
    void deveFiltrarPorFaixaDeAluguel() throws Exception {
        stubValidateOk();
        salvarApartamento("Barato", "Curitiba", "Centro", 1, "900.00", true);
        salvarApartamento("Medio", "Curitiba", "Batel", 2, "1500.00", true);
        salvarApartamento("Caro", "Curitiba", "Batel", 3, "3000.00", true);

        mockMvc
                .perform(
                        get("/api/v1/apartments")
                                .param("minRent", "1000")
                                .param("maxRent", "2000")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Medio"));
    }

    @Test
    void deveFiltrarPorQuartosEDisponibilidade() throws Exception {
        stubValidateOk();
        salvarApartamento("Dois Quartos Livre", "Curitiba", "Centro", 2, "1600.00", true);
        salvarApartamento("Dois Quartos Ocupado", "Curitiba", "Centro", 2, "1700.00", false);
        salvarApartamento("Um Quarto Livre", "Curitiba", "Centro", 1, "1100.00", true);

        mockMvc
                .perform(
                        get("/api/v1/apartments")
                                .param("bedrooms", "2")
                                .param("available", "true")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Dois Quartos Livre"));
    }

    @Test
    void deveFiltrarPorCombinacaoDeFiltros() throws Exception {
        stubValidateOk();
        salvarApartamento("Match", "Curitiba", "Batel", 2, "1800.00", true);
        salvarApartamento("Outro Bairro", "Curitiba", "Centro", 2, "1800.00", true);
        salvarApartamento("Outra Cidade", "Londrina", "Batel", 2, "1800.00", true);

        mockMvc
                .perform(
                        get("/api/v1/apartments")
                                .param("city", "Curitiba")
                                .param("neighborhood", "Batel")
                                .param("bedrooms", "2")
                                .param("minRent", "1500")
                                .param("maxRent", "2000")
                                .param("available", "true")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Match"));
    }

    private void stubValidateOk() {
        userService.stubFor(
                WireMock.post(urlEqualTo("/api/v1/auth/validate"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(
                                                """
                                                {"userId":%d,"email":"arthur@campusconnect.dev","name":"Arthur"}
                                                """
                                                        .formatted(USER_ID))));
    }

    private Map<String, Object> apartamentoValido() {
        return Map.ofEntries(
                Map.entry("name", "Kitnet Centro"),
                Map.entry("description", "Perto da universidade"),
                Map.entry("address", "Rua das Flores"),
                Map.entry("number", "100"),
                Map.entry("complement", "Apto 12"),
                Map.entry("neighborhood", "Centro"),
                Map.entry("city", "Curitiba"),
                Map.entry("state", "PR"),
                Map.entry("cep", "80010-000"),
                Map.entry("bedrooms", 1),
                Map.entry("bathrooms", 1),
                Map.entry("rentPrice", new BigDecimal("1200.00")),
                Map.entry("condominiumPrice", new BigDecimal("200.00")));
    }

    private void salvarApartamento(
            String name,
            String city,
            String neighborhood,
            int bedrooms,
            String rentPrice,
            boolean available) {
        Apartment apartment =
                new Apartment(
                        name,
                        "Descricao",
                        "Rua Teste",
                        "10",
                        null,
                        neighborhood,
                        city,
                        "PR",
                        "80000-000",
                        bedrooms,
                        1,
                        new BigDecimal(rentPrice),
                        new BigDecimal("100.00"),
                        USER_ID);
        apartment.setAvailable(available);
        apartmentRepository.save(apartment);
    }
}
