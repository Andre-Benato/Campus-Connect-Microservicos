package com.campusconnect.ride.application;

import com.campusconnect.common.BusinessException;
import com.campusconnect.common.ForbiddenOperationException;
import com.campusconnect.common.ResourceNotFoundException;
import com.campusconnect.ride.infrastructure.adapters.in.web.dto.RideCreateRequest;
import com.campusconnect.ride.infrastructure.adapters.in.web.dto.RideFilter;
import com.campusconnect.ride.infrastructure.adapters.in.web.dto.RideResponse;
import com.campusconnect.ride.infrastructure.adapters.in.web.dto.RideUpdateRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusconnect.ride.domain.Ride;
import com.campusconnect.ride.infrastructure.adapters.out.persistence.RideRepository;
import com.campusconnect.ride.domain.RideRequest;
import com.campusconnect.ride.infrastructure.adapters.out.persistence.RideRequestRepository;
import com.campusconnect.ride.domain.RideRequestStatus;
import com.campusconnect.ride.infrastructure.adapters.out.persistence.RideSpecifications;
import com.campusconnect.ride.domain.RideStatus;
@Service
public class RideService {

    private final RideRepository rideRepository;
    private final RideRequestRepository rideRequestRepository;

    public RideService(RideRepository rideRepository, RideRequestRepository rideRequestRepository) {
        this.rideRepository = rideRepository;
        this.rideRequestRepository = rideRequestRepository;
    }

    @Transactional
    public RideResponse criar(RideCreateRequest request, Long driverUserId) {
        Ride ride =
                new Ride(
                        driverUserId,
                        request.origin().trim(),
                        request.destination().trim(),
                        request.departureAt(),
                        request.totalSeats(),
                        request.pricePerSeat(),
                        trimOrNull(request.notes()));
        return RideResponse.from(rideRepository.save(ride));
    }

    @Transactional(readOnly = true)
    public List<RideResponse> listar(RideFilter filter) {
        return rideRepository.findAll(RideSpecifications.from(filter)).stream()
                .map(RideResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RideResponse> listarDoMotorista(Long driverUserId) {
        return rideRepository.findByDriverUserIdOrderByDepartureAtAsc(driverUserId).stream()
                .map(RideResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RideResponse buscarPorId(Long id) {
        return RideResponse.from(buscarEntidade(id));
    }

    @Transactional
    public RideResponse atualizar(Long id, RideUpdateRequest request, Long usuarioId) {
        Ride ride = buscarEntidade(id);
        garantirMotorista(ride, usuarioId);
        garantirAberta(ride);

        int ocupados = ride.getTotalSeats() - ride.getAvailableSeats();
        if (request.totalSeats() < ocupados) {
            throw new BusinessException(
                    "totalSeats nao pode ser menor que os assentos ja aceitos (" + ocupados + ")");
        }

        ride.setOrigin(request.origin().trim());
        ride.setDestination(request.destination().trim());
        ride.setDepartureAt(request.departureAt());
        ride.setTotalSeats(request.totalSeats());
        ride.setAvailableSeats(request.totalSeats() - ocupados);
        ride.setPricePerSeat(request.pricePerSeat());
        ride.setNotes(trimOrNull(request.notes()));

        return RideResponse.from(ride);
    }

    @Transactional
    public RideResponse cancelar(Long id, Long usuarioId) {
        Ride ride = buscarEntidade(id);
        garantirMotorista(ride, usuarioId);
        garantirAberta(ride);

        List<RideRequest> ativas =
                rideRequestRepository.findByRideIdAndStatusIn(
                        id, List.of(RideRequestStatus.PENDING, RideRequestStatus.ACCEPTED));

        for (RideRequest solicitacao : ativas) {
            if (solicitacao.getStatus() == RideRequestStatus.ACCEPTED) {
                ride.setAvailableSeats(ride.getAvailableSeats() + solicitacao.getSeatsRequested());
            }
            solicitacao.setStatus(RideRequestStatus.CANCELLED);
        }

        ride.setStatus(RideStatus.CANCELLED);
        return RideResponse.from(ride);
    }

    @Transactional
    public RideResponse encerrar(Long id, Long usuarioId) {
        Ride ride = buscarEntidade(id);
        garantirMotorista(ride, usuarioId);
        garantirAberta(ride);
        ride.setStatus(RideStatus.COMPLETED);
        return RideResponse.from(ride);
    }

    Ride buscarEntidade(Long id) {
        return rideRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carona " + id + " nao encontrada"));
    }

    void garantirMotorista(Ride ride, Long usuarioId) {
        if (!ride.getDriverUserId().equals(usuarioId)) {
            throw new ForbiddenOperationException("Apenas o motorista pode realizar esta operacao");
        }
    }

    void garantirAberta(Ride ride) {
        if (ride.getStatus() != RideStatus.OPEN) {
            throw new BusinessException("Carona nao esta aberta (status=" + ride.getStatus() + ")");
        }
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
