package com.campusconnect.user.infrastructure.adapters.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.campusconnect.user.domain.User;
import com.campusconnect.user.infrastructure.adapters.out.persistence.UserRepository;
/**
 * Exercita o CRUD completo do usuario contra um SQL Server real, validando tambem as migracoes do
 * Flyway e o mapeamento JPA.
 *
 * <p>Com a autenticacao ativa (RF02), apenas o cadastro e publico: as demais operacoes usam o token
 * obtido no login.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserCrudIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void limparBase() {
        userRepository.deleteAll();
    }

    @Test
    void deveCriarUsuarioERetornar201ComLocation() throws Exception {
        mockMvc
                .perform(
                        post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Arthur Markowicz Lopes",
                                          "email": "arthur@campusconnect.dev",
                                          "password": "senha-segura-123"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Arthur Markowicz Lopes"))
                .andExpect(jsonPath("$.email").value("arthur@campusconnect.dev"))
                .andExpect(jsonPath("$.createAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void deveGravarSenhaComHashBcryptENuncaEmTextoPuro() throws Exception {
        long id = criarUsuario("Gustavo Tissot Franca", "gustavo@campusconnect.dev", SENHA);

        User salvo = userRepository.findById(id).orElseThrow();
        assertThat(salvo.getPassword()).isNotEqualTo(SENHA).startsWith("$2a$");
        assertThat(passwordEncoder.matches(SENHA, salvo.getPassword())).isTrue();
    }

    @Test
    void deveListarUsuarios() throws Exception {
        criarUsuario("Andre Gabriel Benato", "andre@campusconnect.dev", SENHA);
        criarUsuario("Luigi Morgenstern Milani", "luigi@campusconnect.dev", SENHA);
        String token = login("andre@campusconnect.dev", SENHA);

        mockMvc
                .perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void deveBuscarUsuarioPorId() throws Exception {
        long id = criarUsuario("Andre Gabriel Benato", "andre@campusconnect.dev", SENHA);
        String token = login("andre@campusconnect.dev", SENHA);

        mockMvc
                .perform(get("/api/v1/users/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.email").value("andre@campusconnect.dev"));
    }

    @Test
    void deveAtualizarNomeEEmailMantendoASenhaQuandoNaoInformada() throws Exception {
        long id = criarUsuario("Nome Antigo", "antigo@campusconnect.dev", SENHA);
        String token = login("antigo@campusconnect.dev", SENHA);
        String hashAnterior = userRepository.findById(id).orElseThrow().getPassword();

        mockMvc
                .perform(
                        put("/api/v1/users/{id}", id)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Nome Novo",
                                          "email": "novo@campusconnect.dev"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nome Novo"))
                .andExpect(jsonPath("$.email").value("novo@campusconnect.dev"));

        assertThat(userRepository.findById(id).orElseThrow().getPassword()).isEqualTo(hashAnterior);
    }

    @Test
    void deveTrocarASenhaQuandoInformadaNaAtualizacao() throws Exception {
        long id = criarUsuario("Nome", "troca@campusconnect.dev", SENHA);
        String token = login("troca@campusconnect.dev", SENHA);

        mockMvc
                .perform(
                        put("/api/v1/users/{id}", id)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Nome",
                                          "email": "troca@campusconnect.dev",
                                          "password": "nova-senha-456"
                                        }
                                        """))
                .andExpect(status().isOk());

        String novoHash = userRepository.findById(id).orElseThrow().getPassword();
        assertThat(passwordEncoder.matches("nova-senha-456", novoHash)).isTrue();
        assertThat(passwordEncoder.matches(SENHA, novoHash)).isFalse();
    }

    @Test
    void deveRemoverUsuario() throws Exception {
        long id = criarUsuario("Para Remover", "remover@campusconnect.dev", SENHA);
        String token = login("remover@campusconnect.dev", SENHA);

        mockMvc
                .perform(delete("/api/v1/users/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(userRepository.existsById(id)).isFalse();
    }

    @Test
    void deveRetornar404ParaUsuarioInexistente() throws Exception {
        criarUsuario("Existente", "existente@campusconnect.dev", SENHA);
        String token = login("existente@campusconnect.dev", SENHA);

        mockMvc
                .perform(get("/api/v1/users/{id}", 999_999).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveExigirTokenNasOperacoesQueNaoSaoOCadastro() throws Exception {
        long id = criarUsuario("Sem Token", "semtoken@campusconnect.dev", SENHA);

        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/{id}", id)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/users/{id}", id)).andExpect(status().isUnauthorized());
        mockMvc
                .perform(
                        put("/api/v1/users/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Nome",
                                          "email": "semtoken@campusconnect.dev"
                                        }
                                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void naoDevePermitirAlterarOuRemoverOCadastroDeOutroUsuario() throws Exception {
        criarUsuario("Dono do Token", "dono@campusconnect.dev", SENHA);
        long idDeOutro = criarUsuario("Outra Pessoa", "outra@campusconnect.dev", SENHA);
        String token = login("dono@campusconnect.dev", SENHA);

        mockMvc
                .perform(
                        put("/api/v1/users/{id}", idDeOutro)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Invasor",
                                          "email": "invasor@campusconnect.dev"
                                        }
                                        """))
                .andExpect(status().isForbidden());

        mockMvc
                .perform(
                        delete("/api/v1/users/{id}", idDeOutro).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(userRepository.existsById(idDeOutro)).isTrue();
    }

    @Test
    void deveRetornar409QuandoOEmailJaEstiverCadastrado() throws Exception {
        criarUsuario("Primeiro", "duplicado@campusconnect.dev", SENHA);

        mockMvc
                .perform(
                        post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Segundo",
                                          "email": "DUPLICADO@campusconnect.dev",
                                          "password": "senha-segura-123"
                                        }
                                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void deveRetornar400ComDetalhesQuandoOsCamposForemInvalidos() throws Exception {
        mockMvc
                .perform(
                        post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "",
                                          "email": "nao-e-um-email",
                                          "password": "curta"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    private long criarUsuario(String name, String email, String password) throws Exception {
        String body =
                objectMapper.writeValueAsString(
                        java.util.Map.of("name", name, "email", email, "password", password));

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
        String body = objectMapper.writeValueAsString(java.util.Map.of("email", email, "password", password));

        String json =
                mockMvc
                        .perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return objectMapper.readTree(json).get("accessToken").asString();
    }
}
