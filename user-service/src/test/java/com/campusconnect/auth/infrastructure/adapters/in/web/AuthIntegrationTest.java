package com.campusconnect.auth.infrastructure.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusconnect.security.JwtConfig;
import com.campusconnect.security.JwtProperties;
import com.campusconnect.user.infrastructure.adapters.out.persistence.UserRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.testcontainers.mssqlserver.MSSQLServerContainer;

import com.campusconnect.auth.infrastructure.adapters.out.persistence.RevokedTokenRepository;
/** Cobre o login, o uso do token nas rotas protegidas e o logout (RF02). */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {

    private static final String NOME = "Arthur Markowicz Lopes";
    private static final String EMAIL = "arthur@campusconnect.dev";
    private static final String SENHA = "senha-segura-123";

    // As migracoes do Flyway sao T-SQL (o ambiente e o Azure SQL Database), por isso o
    // teste precisa de um SQL Server de verdade. A imagem exige aceitar a licenca da
    // Microsoft e pesa ~1,5 GB: o primeiro `mvn test` depois de um clone demora.
    @Container
    @ServiceConnection
    static MSSQLServerContainer sqlServer =
            new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RevokedTokenRepository revokedTokenRepository;
    @Autowired
    private JwtEncoder jwtEncoder;
    @Autowired
    private JwtProperties jwtProperties;

    @BeforeEach
    void limparBase() {
        revokedTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void deveAutenticarERetornarUmTokenComValidadeDeCincoHoras() throws Exception {
        long id = criarUsuario(NOME, EMAIL, SENHA);

        String json =
                mockMvc
                        .perform(loginRequest(EMAIL, SENHA))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.accessToken").isNotEmpty())
                        .andExpect(jsonPath("$.tokenType").value("Bearer"))
                        .andExpect(jsonPath("$.expiresIn").value(Duration.ofHours(5).toSeconds()))
                        .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                        .andExpect(jsonPath("$.user.id").value(id))
                        .andExpect(jsonPath("$.user.email").value(EMAIL))
                        .andExpect(jsonPath("$.user.password").doesNotExist())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(jwtProperties.getExpiration()).isEqualTo(Duration.ofHours(5));
        assertThat(objectMapper.readTree(json).get("accessToken").asString()).contains(".");
    }

    @Test
    void deveAceitarOEmailEmQualquerCaixa() throws Exception {
        criarUsuario(NOME, EMAIL, SENHA);

        mockMvc.perform(loginRequest(EMAIL.toUpperCase(), SENHA)).andExpect(status().isOk());
    }

    @Test
    void deveRecusarSenhaIncorretaSemRevelarQueOEmailExiste() throws Exception {
        criarUsuario(NOME, EMAIL, SENHA);

        mockMvc
                .perform(loginRequest(EMAIL, "senha-errada-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha invalidos"));
    }

    @Test
    void deveRecusarEmailInexistenteComAMesmaRespostaDeSenhaIncorreta() throws Exception {
        mockMvc
                .perform(loginRequest("nao-existe@campusconnect.dev", SENHA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha invalidos"));
    }

    @Test
    void deveRecusarLoginComCamposInvalidos() throws Exception {
        mockMvc
                .perform(loginRequest("nao-e-um-email", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void oTokenDoLoginDeveDarAcessoAsRotasProtegidas() throws Exception {
        long id = criarUsuario(NOME, EMAIL, SENHA);
        String token = login(EMAIL, SENHA);

        mockMvc
                .perform(get("/api/v1/users/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void deveResponder401EmFormatoProblemDetailsQuandoNaoHaToken() throws Exception {
        mockMvc
                .perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Nao autenticado"));
    }

    @Test
    void oLogoutDeveInvalidarOTokenImediatamente() throws Exception {
        long id = criarUsuario(NOME, EMAIL, SENHA);
        String token = login(EMAIL, SENHA);

        mockMvc
                .perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(revokedTokenRepository.count()).isEqualTo(1);

        mockMvc
                .perform(get("/api/v1/users/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oLogoutRepetidoComOMesmoTokenNaoDeveFalharNemDuplicarORegistro() throws Exception {
        criarUsuario(NOME, EMAIL, SENHA);
        String token = login(EMAIL, SENHA);

        mockMvc
                .perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc
                .perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        assertThat(revokedTokenRepository.count()).isEqualTo(1);
    }

    @Test
    void oLogoutNaoDeveAfetarOsDemaisTokensDoMesmoUsuario() throws Exception {
        long id = criarUsuario(NOME, EMAIL, SENHA);
        String tokenDoCelular = login(EMAIL, SENHA);
        String tokenDoNotebook = login(EMAIL, SENHA);

        mockMvc
                .perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + tokenDoCelular))
                .andExpect(status().isNoContent());

        mockMvc
                .perform(
                        get("/api/v1/users/{id}", id).header("Authorization", "Bearer " + tokenDoNotebook))
                .andExpect(status().isOk());
    }

    @Test
    void oLogoutDeveExigirAutenticacao() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    void deveValidarTokenValidoERetornarDadosDoUsuario() throws Exception {
        long id = criarUsuario(NOME, EMAIL, SENHA);
        String token = login(EMAIL, SENHA);

        mockMvc
                .perform(post("/api/v1/auth/validate").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(id))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.name").value(NOME));
    }

    @Test
    void deveRecusarValidateSemToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/validate")).andExpect(status().isUnauthorized());
    }

    @Test
    void deveRecusarValidateAposLogout() throws Exception {
        criarUsuario(NOME, EMAIL, SENHA);
        String token = login(EMAIL, SENHA);

        mockMvc
                .perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc
                .perform(post("/api/v1/auth/validate").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRecusarTokenExpirado() throws Exception {
        long id = criarUsuario(NOME, EMAIL, SENHA);
        Instant agora = Instant.now();
        String expirado =
                assinar(
                        jwtEncoder,
                        jwtProperties.getIssuer(),
                        id,
                        agora.minus(Duration.ofHours(6)),
                        agora.minus(Duration.ofHours(1)));

        mockMvc
                .perform(get("/api/v1/users/{id}", id).header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRecusarTokenAssinadoComOutroSecret() throws Exception {
        long id = criarUsuario(NOME, EMAIL, SENHA);
        JwtEncoder outroEncoder =
                new NimbusJwtEncoder(
                        new ImmutableSecret<>(
                                new SecretKeySpec(
                                        "outro-secret-de-teste-com-mais-de-32-caracteres"
                                                .getBytes(StandardCharsets.UTF_8),
                                        "HmacSHA256")));
        Instant agora = Instant.now();
        String forjado =
                assinar(
                        outroEncoder,
                        jwtProperties.getIssuer(),
                        id,
                        agora,
                        agora.plus(Duration.ofHours(5)));

        mockMvc
                .perform(get("/api/v1/users/{id}", id).header("Authorization", "Bearer " + forjado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRecusarTokenDeOutroEmissor() throws Exception {
        long id = criarUsuario(NOME, EMAIL, SENHA);
        Instant agora = Instant.now();
        String outroEmissor =
                assinar(jwtEncoder, "outro-sistema", id, agora, agora.plus(Duration.ofHours(5)));

        mockMvc
                .perform(get("/api/v1/users/{id}", id).header("Authorization", "Bearer " + outroEmissor))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aRemocaoDoUsuarioDeveLevarJuntoOsTokensRevogadosDele() throws Exception {
        long id = criarUsuario(NOME, EMAIL, SENHA);
        String tokenParaRevogar = login(EMAIL, SENHA);
        String tokenParaRemover = login(EMAIL, SENHA);

        mockMvc
                .perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + tokenParaRevogar))
                .andExpect(status().isNoContent());
        assertThat(revokedTokenRepository.count()).isEqualTo(1);

        mockMvc
                .perform(
                        delete("/api/v1/users/{id}", id)
                                .header("Authorization", "Bearer " + tokenParaRemover))
                .andExpect(status().isNoContent());

        assertThat(revokedTokenRepository.count()).isZero();
    }

    private static String assinar(
            JwtEncoder encoder, String issuer, long userId, Instant emitidoEm, Instant expiraEm) {
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .issuedAt(emitidoEm)
                        .expiresAt(expiraEm)
                        .subject(String.valueOf(userId))
                        .id(UUID.randomUUID().toString())
                        .build();
        return encoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(JwtConfig.ALGORITMO).build(), claims))
                .getTokenValue();
    }

    private MockHttpServletRequestBuilder loginRequest(String email, String password)
            throws Exception {
        return post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password)));
    }

    private long criarUsuario(String name, String email, String password) throws Exception {
        String body =
                objectMapper.writeValueAsString(
                        Map.of("name", name, "email", email, "password", password));

        String json =
                mockMvc
                        .perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        JsonNode node = objectMapper.readTree(json);
        return node.get("id").asLong();
    }

    private String login(String email, String password) throws Exception {
        String json =
                mockMvc
                        .perform(loginRequest(email, password))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return objectMapper.readTree(json).get("accessToken").asString();
    }
}
