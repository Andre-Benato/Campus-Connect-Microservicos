package com.campusconnect.apartment.infrastructure.adapters.in.web;

import com.campusconnect.apartment.infrastructure.adapters.in.web.dto.ApartmentCreateRequest;
import com.campusconnect.apartment.infrastructure.adapters.in.web.dto.ApartmentFilter;
import com.campusconnect.apartment.infrastructure.adapters.in.web.dto.ApartmentResponse;
import com.campusconnect.apartment.infrastructure.adapters.in.web.dto.ApartmentUpdateRequest;
import com.campusconnect.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.campusconnect.apartment.application.ApartmentService;
@RestController
@RequestMapping("/api/v1/apartments")
@Tag(name = "Apartamentos", description = "CRUD de anúncios de apartamento (Azure SQL)")
public class ApartmentController {

    private final ApartmentService apartmentService;

    public ApartmentController(ApartmentService apartmentService) {
        this.apartmentService = apartmentService;
    }

    @PostMapping
    @Operation(summary = "Criar anúncio")
    @ApiResponse(responseCode = "201", description = "Apartamento criado")
    public ResponseEntity<ApartmentResponse> criar(
            @Valid @RequestBody ApartmentCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser usuario) {

        ApartmentResponse criado = apartmentService.criar(request, usuario.id());

        return ResponseEntity.created(URI.create("/api/v1/apartments/" + criado.id())).body(criado);
    }

    @GetMapping
    @Operation(summary = "Listar anúncios", description = "Filtros opcionais por cidade, bairro, aluguel e quartos.")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    public ResponseEntity<List<ApartmentResponse>> listar(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String neighborhood,
            @RequestParam(required = false) BigDecimal minRent,
            @RequestParam(required = false) BigDecimal maxRent,
            @RequestParam(required = false) Integer bedrooms,
            @RequestParam(required = false) Boolean available) {

        ApartmentFilter filter =
                new ApartmentFilter(city, neighborhood, minRent, maxRent, bedrooms, available);
        return ResponseEntity.ok(apartmentService.listar(filter));
    }

    @GetMapping("/my")
    @Operation(summary = "Listar meus anúncios")
    @ApiResponse(responseCode = "200", description = "Lista do usuário autenticado")
    public ResponseEntity<List<ApartmentResponse>> listarMeus(
            @AuthenticationPrincipal AuthenticatedUser usuario) {

        return ResponseEntity.ok(apartmentService.listarDoUsuario(usuario.id()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar anúncio por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Apartamento encontrado"),
        @ApiResponse(responseCode = "404", description = "Apartamento não encontrado")
    })
    public ResponseEntity<ApartmentResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(apartmentService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar próprio anúncio")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Apartamento atualizado"),
        @ApiResponse(responseCode = "403", description = "Sem permissão"),
        @ApiResponse(responseCode = "404", description = "Apartamento não encontrado")
    })
    public ResponseEntity<ApartmentResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ApartmentUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser usuario) {

        return ResponseEntity.ok(apartmentService.atualizar(id, request, usuario.id()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover próprio anúncio")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Apartamento removido"),
        @ApiResponse(responseCode = "403", description = "Sem permissão"),
        @ApiResponse(responseCode = "404", description = "Apartamento não encontrado")
    })
    public ResponseEntity<Void> remover(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser usuario) {

        apartmentService.remover(id, usuario.id());
        return ResponseEntity.noContent().build();
    }
}
