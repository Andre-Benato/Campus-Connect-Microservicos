package com.campusconnect.ride.application.ports;

import java.time.Instant;

/**
 * Evento de dominio publicado no Azure Service Bus quando o motorista aceita uma solicitacao.
 *
 * <p>Contrato consumido pelo chat-service (mesmo JSON).
 */
public record RideRequestAcceptedEvent(
        Long rideId,
        Long requestId,
        Long driverUserId,
        Long passengerUserId,
        String origin,
        String destination,
        Integer seatsAccepted,
        Instant acceptedAt) {

    public static final String TYPE = "RideRequestAccepted";
}
