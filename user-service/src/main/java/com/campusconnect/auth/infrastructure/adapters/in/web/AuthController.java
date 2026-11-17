package com.campusconnect.auth.infrastructure.adapters.in.web;

import com.campusconnect.auth.infrastructure.adapters.in.web.dto.LoginRequest;
import com.campusconnect.auth.infrastructure.adapters.in.web.dto.LoginResponse;
import com.campusconnect.auth.infrastructure.adapters.in.web.dto.TokenValidateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.auth.application.AuthService;
/** Endpoints de autenticacao (RF02). */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Login, validação e logout JWT (RF02)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Troca e-mail e senha por um token de acesso. Endpoint publico. */
    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login", description = "Troca e-mail e senha por um Bearer JWT. Público.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token emitido"),
        @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.autenticar(request));
    }

    /**
     * Confirma que o Bearer token e valido (assinatura, issuer, expiracao e denylist).
     * Usado pelos demais microservicos para autenticar requisicoes remotamente.
     */
    @PostMapping("/validate")
    @Operation(
            summary = "Validar token",
            description = "Usado pelos demais microserviços para autenticar requisições remotamente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token válido"),
        @ApiResponse(responseCode = "401", description = "Token inválido ou expirado")
    })
    public ResponseEntity<TokenValidateResponse> validate(@AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok(TokenValidateResponse.from(token));
    }

    /**
     * Invalida o token enviado na requisicao. Exige autenticacao: o token a ser invalidado e
     * justamente o que autentica a chamada.
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalida o Bearer token enviado (denylist).")
    @ApiResponse(responseCode = "204", description = "Token invalidado")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt token) {
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }
}
