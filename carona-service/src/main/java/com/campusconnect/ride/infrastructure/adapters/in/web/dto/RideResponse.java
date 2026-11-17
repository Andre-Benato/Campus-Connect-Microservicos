package com.campusconnect.ride.infrastructure.adapters.in.web.dto;

import com.campusconnect.ride.domain.Ride;
import com.campusconnect.ride.domain.RideStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RideResponse(
        Long id,
        Long driverUserId,
        String origin,
        String destination,
        LocalDateTime departureAt,
        Integer totalSeats,
        Integer availableSeats,
        BigDecimal pricePerSeat,
        String notes,
        RideStatus status) {

    public static RideResponse from(Ride ride) {
        return new RideResponse(
                ride.getId(),
                ride.getDriverUserId(),
                ride.getOrigin(),
                ride.getDestination(),
                ride.getDepartureAt(),
                ride.getTotalSeats(),
                ride.getAvailableSeats(),
                ride.getPricePerSeat(),
                ride.getNotes(),
                ride.getStatus());
    }
}
