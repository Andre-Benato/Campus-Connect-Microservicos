package com.campusconnect.ride.infrastructure.adapters.in.web.dto;

import com.campusconnect.ride.domain.RideRequest;
import com.campusconnect.ride.domain.RideRequestStatus;
import java.time.LocalDateTime;

public record RideRequestResponse(
        Long id,
        Long rideId,
        Long passengerUserId,
        Integer seatsRequested,
        String message,
        RideRequestStatus status,
        LocalDateTime createdAt) {

    public static RideRequestResponse from(RideRequest request) {
        return new RideRequestResponse(
                request.getId(),
                request.getRideId(),
                request.getPassengerUserId(),
                request.getSeatsRequested(),
                request.getMessage(),
                request.getStatus(),
                request.getCreatedAt());
    }
}
