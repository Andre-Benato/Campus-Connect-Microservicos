package com.campusconnect.ride.infrastructure.adapters.in.web;

import com.campusconnect.ride.infrastructure.adapters.in.web.dto.RideCreateRequest;
import com.campusconnect.ride.infrastructure.adapters.in.web.dto.RideFilter;
import com.campusconnect.ride.infrastructure.adapters.in.web.dto.RideRequestCreateRequest;
import com.campusconnect.ride.infrastructure.adapters.in.web.dto.RideRequestResponse;
import com.campusconnect.ride.infrastructure.adapters.in.web.dto.RideResponse;
import com.campusconnect.ride.infrastructure.adapters.in.web.dto.RideUpdateRequest;
import com.campusconnect.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.campusconnect.ride.application.RideRequestService;
import com.campusconnect.ride.application.RideService;
@RestController
@RequestMapping("/api/v1/rides")
@Tag(name = "Caronas", description = "Ofertas de carona e solicitações (Azure SQL)")
public class RideController {

    private final RideService rideService;
    private final RideRequestService rideRequestService;

    public RideController(RideService rideService, RideRequestService rideRequestService) {
        this.rideService = rideService;
        this.rideRequestService = rideRequestService;
    }

    @PostMapping
    @Operation(summary = "Criar oferta de carona")
    @ApiResponse(responseCode = "201", description = "Carona criada")
    public ResponseEntity<RideResponse> criar(
            @Valid @RequestBody RideCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser usuario) {

        RideResponse criado = rideService.criar(request, usuario.id());
        return ResponseEntity.created(URI.create("/api/v1/rides/" + criado.id())).body(criado);
    }

    @GetMapping
    @Operation(summary = "Listar caronas", description = "Filtros opcionais por origem, destino e horário.")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    public ResponseEntity<List<RideResponse>> listar(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime departureFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime departureTo,
            @RequestParam(required = false) Boolean onlyAvailable) {

        RideFilter filter =
                new RideFilter(origin, destination, departureFrom, departureTo, onlyAvailable);
        return ResponseEntity.ok(rideService.listar(filter));
    }

    @GetMapping("/my")
    @Operation(summary = "Listar minhas ofertas (motorista)")
    @ApiResponse(responseCode = "200", description = "Lista do motorista autenticado")
    public ResponseEntity<List<RideResponse>> listarMinhas(
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        return ResponseEntity.ok(rideService.listarDoMotorista(usuario.id()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar carona por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Carona encontrada"),
        @ApiResponse(responseCode = "404", description = "Carona não encontrada")
    })
    public ResponseEntity<RideResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(rideService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar própria oferta")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Carona atualizada"),
        @ApiResponse(responseCode = "403", description = "Sem permissão"),
        @ApiResponse(responseCode = "404", description = "Carona não encontrada")
    })
    public ResponseEntity<RideResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody RideUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        return ResponseEntity.ok(rideService.atualizar(id, request, usuario.id()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancelar oferta")
    @ApiResponse(responseCode = "200", description = "Carona cancelada")
    public ResponseEntity<RideResponse> cancelar(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser usuario) {
        return ResponseEntity.ok(rideService.cancelar(id, usuario.id()));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Encerrar carona")
    @ApiResponse(responseCode = "200", description = "Carona encerrada")
    public ResponseEntity<RideResponse> encerrar(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser usuario) {
        return ResponseEntity.ok(rideService.encerrar(id, usuario.id()));
    }

    @PostMapping("/{id}/requests")
    @Operation(summary = "Solicitar vaga na carona")
    @ApiResponse(responseCode = "201", description = "Solicitação criada")
    public ResponseEntity<RideRequestResponse> solicitar(
            @PathVariable Long id,
            @Valid @RequestBody RideRequestCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser usuario) {

        RideRequestResponse criado = rideRequestService.solicitar(id, request, usuario.id());
        return ResponseEntity.created(
                        URI.create("/api/v1/rides/" + id + "/requests/" + criado.id()))
                .body(criado);
    }

    @GetMapping("/{id}/requests")
    @Operation(summary = "Listar solicitações da carona (motorista)")
    @ApiResponse(responseCode = "200", description = "Lista de solicitações")
    public ResponseEntity<List<RideRequestResponse>> listarSolicitacoes(
            @PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser usuario) {
        return ResponseEntity.ok(rideRequestService.listarDaCarona(id, usuario.id()));
    }

    @PostMapping("/{id}/requests/{requestId}/accept")
    @Operation(summary = "Aceitar solicitação")
    @ApiResponse(responseCode = "200", description = "Solicitação aceita")
    public ResponseEntity<RideRequestResponse> aceitar(
            @PathVariable Long id,
            @PathVariable Long requestId,
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        return ResponseEntity.ok(rideRequestService.aceitar(id, requestId, usuario.id()));
    }

    @PostMapping("/{id}/requests/{requestId}/reject")
    @Operation(summary = "Recusar solicitação")
    @ApiResponse(responseCode = "200", description = "Solicitação recusada")
    public ResponseEntity<RideRequestResponse> recusar(
            @PathVariable Long id,
            @PathVariable Long requestId,
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        return ResponseEntity.ok(rideRequestService.recusar(id, requestId, usuario.id()));
    }

    @PostMapping("/{id}/requests/{requestId}/cancel")
    @Operation(summary = "Cancelar própria solicitação (passageiro)")
    @ApiResponse(responseCode = "200", description = "Solicitação cancelada")
    public ResponseEntity<RideRequestResponse> cancelarSolicitacao(
            @PathVariable Long id,
            @PathVariable Long requestId,
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        return ResponseEntity.ok(rideRequestService.cancelar(id, requestId, usuario.id()));
    }
}
