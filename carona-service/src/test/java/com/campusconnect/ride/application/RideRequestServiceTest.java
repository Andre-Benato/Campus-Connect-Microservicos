package com.campusconnect.ride.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusconnect.common.BusinessException;
import com.campusconnect.ride.application.ports.RideRequestAcceptedEvent;
import com.campusconnect.ride.application.ports.RideRequestAcceptedPublisher;
import com.campusconnect.ride.infrastructure.adapters.in.web.dto.RideRequestCreateRequest;
import com.campusconnect.ride.infrastructure.adapters.in.web.dto.RideRequestResponse;
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
import com.campusconnect.ride.domain.RideRequest;
import com.campusconnect.ride.infrastructure.adapters.out.persistence.RideRequestRepository;
import com.campusconnect.ride.domain.RideRequestStatus;
@ExtendWith(MockitoExtension.class)
class RideRequestServiceTest {

    @Mock
    private RideService rideService;

    @Mock
    private RideRequestRepository rideRequestRepository;

    @Mock
    private RideRequestAcceptedPublisher rideRequestAcceptedPublisher;

    private RideRequestService rideRequestService;

    @BeforeEach
    void setUp() {
        rideRequestService =
                new RideRequestService(rideService, rideRequestRepository, rideRequestAcceptedPublisher);
    }

    @Test
    void deveRecusarSolicitacaoDoProprioMotorista() {
        Ride ride = caronaAberta(20L, 1L, 3);
        when(rideService.buscarEntidade(20L)).thenReturn(ride);

        assertThatThrownBy(
                        () ->
                                rideRequestService.solicitar(
                                        20L, new RideRequestCreateRequest(1, null), 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Motorista");
        verify(rideRequestRepository, never()).save(any());
    }

    @Test
    void deveRecusarSolicitacaoQuandoJaExisteAtiva() {
        Ride ride = caronaAberta(20L, 1L, 3);
        when(rideService.buscarEntidade(20L)).thenReturn(ride);
        when(rideRequestRepository.existsByRideIdAndPassengerUserIdAndStatusIn(
                        eq(20L),
                        eq(5L),
                        eq(List.of(RideRequestStatus.PENDING, RideRequestStatus.ACCEPTED))))
                .thenReturn(true);

        assertThatThrownBy(
                        () ->
                                rideRequestService.solicitar(
                                        20L, new RideRequestCreateRequest(1, "quero"), 5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ja existe solicitacao ativa");
    }

    @Test
    void deveAceitarSolicitacaoReduzindoAssentosEPublicandoEvento() {
        Ride ride = caronaAberta(20L, 1L, 4);
        RideRequest solicitacao = new RideRequest(20L, 5L, 2, "preciso");
        ReflectionTestUtils.setField(solicitacao, "id", 50L);

        when(rideService.buscarEntidade(20L)).thenReturn(ride);
        when(rideRequestRepository.findByIdAndRideId(50L, 20L)).thenReturn(Optional.of(solicitacao));

        RideRequestResponse response = rideRequestService.aceitar(20L, 50L, 1L);

        assertThat(response.status()).isEqualTo(RideRequestStatus.ACCEPTED);
        assertThat(ride.getAvailableSeats()).isEqualTo(2);

        ArgumentCaptor<RideRequestAcceptedEvent> eventCaptor =
                ArgumentCaptor.forClass(RideRequestAcceptedEvent.class);
        verify(rideRequestAcceptedPublisher).publish(eventCaptor.capture());
        RideRequestAcceptedEvent evento = eventCaptor.getValue();
        assertThat(evento.rideId()).isEqualTo(20L);
        assertThat(evento.requestId()).isEqualTo(50L);
        assertThat(evento.driverUserId()).isEqualTo(1L);
        assertThat(evento.passengerUserId()).isEqualTo(5L);
        assertThat(evento.seatsAccepted()).isEqualTo(2);
    }

    @Test
    void deveCriarSolicitacaoPendenteQuandoHaAssentos() {
        Ride ride = caronaAberta(20L, 1L, 3);
        when(rideService.buscarEntidade(20L)).thenReturn(ride);
        when(rideRequestRepository.existsByRideIdAndPassengerUserIdAndStatusIn(
                        eq(20L),
                        eq(5L),
                        eq(List.of(RideRequestStatus.PENDING, RideRequestStatus.ACCEPTED))))
                .thenReturn(false);
        when(rideRequestRepository.save(any(RideRequest.class)))
                .thenAnswer(
                        invocation -> {
                            RideRequest saved = invocation.getArgument(0);
                            ReflectionTestUtils.setField(saved, "id", 77L);
                            return saved;
                        });

        RideRequestResponse response =
                rideRequestService.solicitar(20L, new RideRequestCreateRequest(1, "  oi  "), 5L);

        assertThat(response.id()).isEqualTo(77L);
        assertThat(response.status()).isEqualTo(RideRequestStatus.PENDING);
        assertThat(response.message()).isEqualTo("oi");
    }

    private static Ride caronaAberta(Long id, Long driverId, int seats) {
        Ride ride =
                new Ride(
                        driverId,
                        "Campus",
                        "Centro",
                        LocalDateTime.now().plusDays(1),
                        seats,
                        new BigDecimal("10.00"),
                        null);
        ReflectionTestUtils.setField(ride, "id", id);
        return ride;
    }
}
