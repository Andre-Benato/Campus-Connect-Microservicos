package com.campusconnect.ride.application;

import com.campusconnect.common.BusinessException;
import com.campusconnect.common.ForbiddenOperationException;
import com.campusconnect.common.ResourceNotFoundException;
import com.campusconnect.ride.application.ports.RideRequestAcceptedEvent;
import com.campusconnect.ride.application.ports.RideRequestAcceptedPublisher;
import com.campusconnect.ride.infrastructure.adapters.in.web.dto.RideRequestCreateRequest;
import com.campusconnect.ride.infrastructure.adapters.in.web.dto.RideRequestResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.campusconnect.ride.domain.Ride;
import com.campusconnect.ride.domain.RideRequest;
import com.campusconnect.ride.infrastructure.adapters.out.persistence.RideRequestRepository;
import com.campusconnect.ride.domain.RideRequestStatus;
import com.campusconnect.ride.domain.RideStatus;
@Service
public class RideRequestService {

    private static final List<RideRequestStatus> STATUS_ATIVOS =
            List.of(RideRequestStatus.PENDING, RideRequestStatus.ACCEPTED);

    private final RideService rideService;
    private final RideRequestRepository rideRequestRepository;
    private final RideRequestAcceptedPublisher rideRequestAcceptedPublisher;

    public RideRequestService(
            RideService rideService,
            RideRequestRepository rideRequestRepository,
            RideRequestAcceptedPublisher rideRequestAcceptedPublisher) {
        this.rideService = rideService;
        this.rideRequestRepository = rideRequestRepository;
        this.rideRequestAcceptedPublisher = rideRequestAcceptedPublisher;
    }

    @Transactional
    public RideRequestResponse solicitar(
            Long rideId, RideRequestCreateRequest request, Long passengerUserId) {
        Ride ride = rideService.buscarEntidade(rideId);
        rideService.garantirAberta(ride);

        if (ride.getDriverUserId().equals(passengerUserId)) {
            throw new BusinessException("Motorista nao pode solicitar a propria carona");
        }
        if (ride.getAvailableSeats() < request.seatsRequested()) {
            throw new BusinessException("Assentos insuficientes na carona");
        }
        if (rideRequestRepository.existsByRideIdAndPassengerUserIdAndStatusIn(
                rideId, passengerUserId, STATUS_ATIVOS)) {
            throw new BusinessException("Ja existe solicitacao ativa para esta carona");
        }

        RideRequest solicitacao =
                new RideRequest(
                        rideId,
                        passengerUserId,
                        request.seatsRequested(),
                        trimOrNull(request.message()));
        return RideRequestResponse.from(rideRequestRepository.save(solicitacao));
    }

    @Transactional(readOnly = true)
    public List<RideRequestResponse> listarDaCarona(Long rideId, Long usuarioId) {
        Ride ride = rideService.buscarEntidade(rideId);
        rideService.garantirMotorista(ride, usuarioId);
        return rideRequestRepository.findByRideIdOrderByCreatedAtAsc(rideId).stream()
                .map(RideRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RideRequestResponse> listarDoPassageiro(Long passengerUserId) {
        return rideRequestRepository.findByPassengerUserIdOrderByCreatedAtDesc(passengerUserId).stream()
                .map(RideRequestResponse::from)
                .toList();
    }

    @Transactional
    public RideRequestResponse aceitar(Long rideId, Long requestId, Long usuarioId) {
        Ride ride = rideService.buscarEntidade(rideId);
        rideService.garantirMotorista(ride, usuarioId);
        rideService.garantirAberta(ride);

        RideRequest solicitacao = buscarSolicitacao(rideId, requestId);
        if (solicitacao.getStatus() != RideRequestStatus.PENDING) {
            throw new BusinessException(
                    "Somente solicitacoes PENDING podem ser aceitas (status="
                            + solicitacao.getStatus()
                            + ")");
        }
        if (ride.getAvailableSeats() < solicitacao.getSeatsRequested()) {
            throw new BusinessException("Assentos insuficientes na carona");
        }

        solicitacao.setStatus(RideRequestStatus.ACCEPTED);
        ride.setAvailableSeats(ride.getAvailableSeats() - solicitacao.getSeatsRequested());

        RideRequestAcceptedEvent evento =
                new RideRequestAcceptedEvent(
                        ride.getId(),
                        solicitacao.getId(),
                        ride.getDriverUserId(),
                        solicitacao.getPassengerUserId(),
                        ride.getOrigin(),
                        ride.getDestination(),
                        solicitacao.getSeatsRequested(),
                        Instant.now());
        publicarAposCommit(evento);

        return RideRequestResponse.from(solicitacao);
    }

    @Transactional
    public RideRequestResponse recusar(Long rideId, Long requestId, Long usuarioId) {
        Ride ride = rideService.buscarEntidade(rideId);
        rideService.garantirMotorista(ride, usuarioId);

        RideRequest solicitacao = buscarSolicitacao(rideId, requestId);
        if (solicitacao.getStatus() != RideRequestStatus.PENDING) {
            throw new BusinessException(
                    "Somente solicitacoes PENDING podem ser recusadas (status="
                            + solicitacao.getStatus()
                            + ")");
        }

        solicitacao.setStatus(RideRequestStatus.REJECTED);
        return RideRequestResponse.from(solicitacao);
    }

    @Transactional
    public RideRequestResponse cancelar(Long rideId, Long requestId, Long usuarioId) {
        Ride ride = rideService.buscarEntidade(rideId);
        RideRequest solicitacao = buscarSolicitacao(rideId, requestId);

        boolean ehPassageiro = solicitacao.getPassengerUserId().equals(usuarioId);
        boolean ehMotorista = ride.getDriverUserId().equals(usuarioId);
        if (!ehPassageiro && !ehMotorista) {
            throw new ForbiddenOperationException(
                    "Apenas o passageiro ou o motorista podem cancelar a solicitacao");
        }

        if (solicitacao.getStatus() != RideRequestStatus.PENDING
                && solicitacao.getStatus() != RideRequestStatus.ACCEPTED) {
            throw new BusinessException(
                    "Solicitacao nao pode ser cancelada (status=" + solicitacao.getStatus() + ")");
        }

        if (solicitacao.getStatus() == RideRequestStatus.ACCEPTED
                && ride.getStatus() == RideStatus.OPEN) {
            ride.setAvailableSeats(ride.getAvailableSeats() + solicitacao.getSeatsRequested());
        }

        solicitacao.setStatus(RideRequestStatus.CANCELLED);
        return RideRequestResponse.from(solicitacao);
    }

    private void publicarAposCommit(RideRequestAcceptedEvent evento) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            rideRequestAcceptedPublisher.publish(evento);
                        }
                    });
            return;
        }
        rideRequestAcceptedPublisher.publish(evento);
    }

    private RideRequest buscarSolicitacao(Long rideId, Long requestId) {
        return rideRequestRepository
                .findByIdAndRideId(requestId, rideId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Solicitacao " + requestId + " nao encontrada na carona " + rideId));
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
