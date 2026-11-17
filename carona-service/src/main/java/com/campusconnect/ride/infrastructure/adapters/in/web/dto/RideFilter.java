package com.campusconnect.ride.infrastructure.adapters.in.web.dto;

import java.time.LocalDateTime;

public record RideFilter(
        String origin,
        String destination,
        LocalDateTime departureFrom,
        LocalDateTime departureTo,
        Boolean onlyAvailable) {}
