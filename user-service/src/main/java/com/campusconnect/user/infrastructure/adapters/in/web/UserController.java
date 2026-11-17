package com.campusconnect.user.infrastructure.adapters.in.web;

import com.campusconnect.security.AuthenticatedUser;
import com.campusconnect.user.infrastructure.adapters.in.web.dto.UserCreateRequest;
import com.campusconnect.user.infrastructure.adapters.in.web.dto.UserResponse;
import com.campusconnect.user.infrastructure.adapters.in.web.dto.UserUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.user.application.UserService;
/**
 * Endpoints REST do CRUD de usuario.
 *
 * <p>Apenas o cadastro (POST) e publico; os demais exigem um token JWT valido, e a alteracao e a
 * remocao ficam restritas ao proprio registro do usuario autenticado.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuários", description = "CRUD de usuários (Azure SQL)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @SecurityRequirements
    @Operation(summary = "Cadastrar usuário", description = "Endpoint público (RF01).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuário criado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    })
    public ResponseEntity<UserResponse> criar(@Valid @RequestBody UserCreateRequest request) {
        UserResponse criado = userService.criar(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + criado.id())).body(criado);
    }

    @GetMapping
    @Operation(summary = "Listar usuários")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    public ResponseEntity<List<UserResponse>> listar() {
        return ResponseEntity.ok(userService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<UserResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(userService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar próprio usuário")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário atualizado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão para alterar outro usuário"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<UserResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok(userService.atualizar(id, request, AuthenticatedUser.id(token)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover próprio usuário")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Usuário removido"),
        @ApiResponse(responseCode = "403", description = "Sem permissão para remover outro usuário"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<Void> remover(@PathVariable Long id, @AuthenticationPrincipal Jwt token) {
        userService.remover(id, AuthenticatedUser.id(token));
        return ResponseEntity.noContent().build();
    }
}
