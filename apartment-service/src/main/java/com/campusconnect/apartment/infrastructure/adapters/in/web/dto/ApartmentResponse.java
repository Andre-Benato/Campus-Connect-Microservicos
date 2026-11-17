package com.campusconnect.apartment.infrastructure.adapters.in.web.dto;

import com.campusconnect.apartment.domain.Apartment;
import java.math.BigDecimal;

public record ApartmentResponse(
        Long id,
        String name,
        String description,
        String address,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String cep,
        Integer bedrooms,
        Integer bathrooms,
        BigDecimal rentPrice,
        BigDecimal condominiumPrice,
        boolean available,
        Long userId
) {

    public static ApartmentResponse from(Apartment apartment) {
        return new ApartmentResponse(
                apartment.getId(),
                apartment.getName(),
                apartment.getDescription(),
                apartment.getAddress(),
                apartment.getNumber(),
                apartment.getComplement(),
                apartment.getNeighborhood(),
                apartment.getCity(),
                apartment.getState(),
                apartment.getCep(),
                apartment.getBedrooms(),
                apartment.getBathrooms(),
                apartment.getRentPrice(),
                apartment.getCondominiumPrice(),
                apartment.isAvailable(),
                apartment.getUserId());
    }
}
