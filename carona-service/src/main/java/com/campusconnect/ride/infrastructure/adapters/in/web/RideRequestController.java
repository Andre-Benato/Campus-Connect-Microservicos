package com.campusconnect.ride.infrastructure.adapters.in.web;

import com.campusconnect.ride.infrastructure.adapters.in.web.dto.RideRequestResponse;
import com.campusconnect.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.ride.application.RideRequestService;
@RestController
@RequestMapping("/api/v1/ride-requests")
@Tag(name = "Solicitações de carona", description = "Consultas do passageiro sobre suas solicitações")
public class RideRequestController {

    private final RideRequestService rideRequestService;

    public RideRequestController(RideRequestService rideRequestService) {
        this.rideRequestService = rideRequestService;
    }

    @GetMapping("/my")
    @Operation(summary = "Listar minhas solicitações (passageiro)")
    @ApiResponse(responseCode = "200", description = "Lista do passageiro autenticado")
    public ResponseEntity<List<RideRequestResponse>> listarMinhas(
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        return ResponseEntity.ok(rideRequestService.listarDoPassageiro(usuario.id()));
    }
}
