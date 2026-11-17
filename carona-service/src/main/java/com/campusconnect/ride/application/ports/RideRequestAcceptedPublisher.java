package com.campusconnect.ride.application.ports;

public interface RideRequestAcceptedPublisher {

    void publish(RideRequestAcceptedEvent event);
}
