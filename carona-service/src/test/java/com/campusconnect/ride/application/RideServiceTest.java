package com.campusconnect.ride.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusconnect.common.BusinessException;
import com.campusconnect.common.ForbiddenOperationException;
import com.campusconnect.common.ResourceNotFoundException;
import com.campusconnect.ride.infrastructure.adapters.in.web.dto.RideCreateRequest;
import com.campusconnect.ride.infrastructure.adapters.in.web.dto.RideResponse;
import com.campusconnect.ride.infrastructure.adapters.in.web.dto.RideUpdateRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.campusconnect.ride.domain.Ride;
import com.campusconnect.ride.infrastructure.adapters.out.persistence.RideRepository;
import com.campusconnect.ride.domain.RideRequest;
import com.campusconnect.ride.infrastructure.adapters.out.persistence.RideRequestRepository;
import com.campusconnect.ride.domain.RideRequestStatus;
import com.campusconnect.ride.domain.RideStatus;
@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RideRequestRepository rideRequestRepository;

    private RideService rideService;

    @BeforeEach
    void setUp() {
        rideService = new RideService(rideRepository, rideRequestRepository);
    }

    @Test
    void deveCriarCaronaAbertaComAssentosDisponiveisIguaisAoTotal() {
        LocalDateTime saida = LocalDateTime.now().plusDays(1);
        RideCreateRequest request =
                new RideCreateRequest(
                        " Campus ", " Centro ", saida, 3, new BigDecimal("15.00"), "  ");

        when(rideRepository.save(any(Ride.class)))
                .thenAnswer(
                        invocation -> {
                            Ride saved = invocation.getArgument(0);
                            ReflectionTestUtils.setField(saved, "id", 20L);
                            return saved;
                        });

        RideResponse response = rideService.criar(request, 9L);

        ArgumentCaptor<Ride> captor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(captor.capture());
        Ride persisted = captor.getValue();

        assertThat(persisted.getDriverUserId()).isEqualTo(9L);
        assertThat(persisted.getOrigin()).isEqualTo("Campus");
        assertThat(persisted.getDestination()).isEqualTo("Centro");
        assertThat(persisted.getAvailableSeats()).isEqualTo(3);
        assertThat(persisted.getStatus()).isEqualTo(RideStatus.OPEN);
        assertThat(persisted.getNotes()).isNull();
        assertThat(response.id()).isEqualTo(20L);
    }

    @Test
    void deveRecusarEncerrarQuandoUsuarioNaoEMotorista() {
        Ride ride = caronaAberta(20L, 1L, 4, 4);
        when(rideRepository.findById(20L)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.encerrar(20L, 99L))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void deveEncerrarCaronaQuandoMotoristaSolicita() {
        Ride ride = caronaAberta(20L, 1L, 4, 2);
        when(rideRepository.findById(20L)).thenReturn(Optional.of(ride));

        RideResponse response = rideService.encerrar(20L, 1L);

        assertThat(response.status()).isEqualTo(RideStatus.COMPLETED);
        assertThat(ride.getStatus()).isEqualTo(RideStatus.COMPLETED);
    }

    @Test
    void deveRecusarAtualizacaoQuandoTotalSeatsMenorQueOcupados() {
        Ride ride = caronaAberta(20L, 1L, 4, 1); // 3 ocupados
        when(rideRepository.findById(20L)).thenReturn(Optional.of(ride));

        RideUpdateRequest request =
                new RideUpdateRequest(
                        "Campus",
                        "Centro",
                        LocalDateTime.now().plusDays(2),
                        2,
                        new BigDecimal("10.00"),
                        null);

        assertThatThrownBy(() -> rideService.atualizar(20L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("totalSeats");
    }

    @Test
    void deveCancelarCaronaESolicitacoesAtivas() {
        Ride ride = caronaAberta(20L, 1L, 4, 2);
        RideRequest aceita = new RideRequest(20L, 5L, 2, null);
        ReflectionTestUtils.setField(aceita, "id", 50L);
        aceita.setStatus(RideRequestStatus.ACCEPTED);
        RideRequest pendente = new RideRequest(20L, 6L, 1, "oi");
        ReflectionTestUtils.setField(pendente, "id", 51L);

        when(rideRepository.findById(20L)).thenReturn(Optional.of(ride));
        when(rideRequestRepository.findByRideIdAndStatusIn(
                        20L, List.of(RideRequestStatus.PENDING, RideRequestStatus.ACCEPTED)))
                .thenReturn(List.of(aceita, pendente));

        RideResponse response = rideService.cancelar(20L, 1L);

        assertThat(response.status()).isEqualTo(RideStatus.CANCELLED);
        assertThat(ride.getAvailableSeats()).isEqualTo(4);
        assertThat(aceita.getStatus()).isEqualTo(RideRequestStatus.CANCELLED);
        assertThat(pendente.getStatus()).isEqualTo(RideRequestStatus.CANCELLED);
    }

    @Test
    void deveLancarNotFoundQuandoCaronaNaoExiste() {
        when(rideRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.buscarPorId(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static Ride caronaAberta(Long id, Long driverId, int total, int available) {
        Ride ride =
                new Ride(
                        driverId,
                        "Campus",
                        "Centro",
                        LocalDateTime.now().plusDays(1),
                        total,
                        new BigDecimal("12.00"),
                        null);
        ReflectionTestUtils.setField(ride, "id", id);
        ride.setAvailableSeats(available);
        return ride;
    }
}
