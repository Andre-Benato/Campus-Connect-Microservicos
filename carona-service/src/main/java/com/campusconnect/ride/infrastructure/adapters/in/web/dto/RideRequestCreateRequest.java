package com.campusconnect.ride.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RideRequestCreateRequest(
        @NotNull @Min(1) Integer seatsRequested, @Size(max = 500) String message) {}
