package com.campusconnect.ride.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RideCreateRequest(
        @NotBlank @Size(max = 200) String origin,
        @NotBlank @Size(max = 200) String destination,
        @NotNull @Future LocalDateTime departureAt,
        @NotNull @Min(1) Integer totalSeats,
        @Positive BigDecimal pricePerSeat,
        @Size(max = 1000) String notes) {}
