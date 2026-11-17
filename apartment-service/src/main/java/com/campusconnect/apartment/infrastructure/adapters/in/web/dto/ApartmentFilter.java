package com.campusconnect.apartment.infrastructure.adapters.in.web.dto;

import java.math.BigDecimal;

public record ApartmentFilter(
        String city,
        String neighborhood,
        BigDecimal minRent,
        BigDecimal maxRent,
        Integer bedrooms,
        Boolean available) {}
