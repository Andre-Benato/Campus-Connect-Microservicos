package com.campusconnect.ride.infrastructure.adapters.out.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.campusconnect.ride.application.ports.RideRequestAcceptedEvent;
import com.campusconnect.ride.application.ports.RideRequestAcceptedPublisher;
/**
 * Fallback quando {@code AZURE_SERVICEBUS_CONNECTION_STRING} nao esta configurada. Permite
 * subir o carona-service localmente sem broker.
 */
public class LoggingRideRequestAcceptedPublisher implements RideRequestAcceptedPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingRideRequestAcceptedPublisher.class);

    @Override
    public void publish(RideRequestAcceptedEvent event) {
        log.info(
                "Service Bus nao configurado; evento {} nao enviado (rideId={}, requestId={}, driver={}, passenger={})",
                RideRequestAcceptedEvent.TYPE,
                event.rideId(),
                event.requestId(),
                event.driverUserId(),
                event.passengerUserId());
    }
}
