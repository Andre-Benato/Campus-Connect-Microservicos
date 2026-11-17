package com.campusconnect.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties({PasswordHashingProperties.class, JwtProperties.class})
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder(PasswordHashingProperties properties) {
        if (PasswordHashingProperties.DEFAULT_DEV_SECRET.equals(properties.getSecret())) {
            log.warn(
                    "Usando o secret de senha default. Defina CAMPUSCONNECT_PASSWORD_SECRET antes de"
                            + " implantar em producao.");
        }
        return new PepperedBCryptPasswordEncoder(properties.getSecret(), properties.getStrength());
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return new ProblemDetailAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return new ProblemDetailAccessDeniedHandler(objectMapper);
    }

    /**
     * A API e stateless: cada requisicao se identifica pelo token JWT no cabecalho
     * {@code Authorization} (RF02), sem sessao no servidor.
     *
     * <p>Apenas dois endpoints ficam abertos, porque sao os unicos que um usuario ainda sem token
     * precisa alcancar: o cadastro (RF01) e o login. Todo o restante exige um token valido.
     *
     * <p>O CSRF permanece desabilitado por nao haver cookie de sessao: o token nao e enviado
     * automaticamente pelo navegador, entao nao existe o cenario que o CSRF protege.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler)
            throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/swagger-ui.html",
                                                "/swagger-ui/**",
                                                "/v3/api-docs",
                                                "/v3/api-docs/**")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/api/v1/users")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2
                                        .jwt(Customizer.withDefaults())
                                        .authenticationEntryPoint(authenticationEntryPoint)
                                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(
                        exceptions ->
                                exceptions
                                        .authenticationEntryPoint(authenticationEntryPoint)
                                        .accessDeniedHandler(accessDeniedHandler))
                .build();
    }
}
