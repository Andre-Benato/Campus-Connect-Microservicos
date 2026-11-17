package com.campusconnect.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI caronaServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Campus Connect — Carona Service")
                        .version("1.0.0")
                        .description("""
                                Microserviço de ofertas e solicitações de carona (Azure SQL).
                                CRUD e fluxo de aceite/recusa; autenticação via Bearer JWT.
                                """)
                        .contact(new Contact()
                                .name("Campus Connect — André, Arthur, Gustavo, Luigi"))
                        .license(new License().name("Uso Acadêmico")))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_SCHEME,
                                new SecurityScheme()
                                        .name(BEARER_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
