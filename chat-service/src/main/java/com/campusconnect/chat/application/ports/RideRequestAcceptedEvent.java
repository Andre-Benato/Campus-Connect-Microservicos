package com.campusconnect.chat.application.ports;

import java.time.Instant;

/**
 * Espelho do contrato publicado pelo carona-service na fila Azure Service Bus.
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
