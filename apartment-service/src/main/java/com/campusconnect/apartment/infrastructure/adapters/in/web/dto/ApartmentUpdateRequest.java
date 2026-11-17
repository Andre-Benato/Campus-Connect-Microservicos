package com.campusconnect.apartment.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ApartmentUpdateRequest(

        @NotBlank
        @Size(max = 120)
        String name,

        @Size(max = 1000)
        String description,

        @NotBlank
        @Size(max = 200)
        String address,

        @NotBlank
        @Size(max = 20)
        String number,

        @Size(max = 100)
        String complement,

        @NotBlank
        @Size(max = 100)
        String neighborhood,

        @NotBlank
        @Size(max = 100)
        String city,

        @NotBlank
        @Size(min = 2, max = 2)
        String state,

        @NotBlank
        @Size(max = 9)
        String cep,

        @NotNull
        @Min(0)
        Integer bedrooms,

        @NotNull
        @Min(0)
        Integer bathrooms,

        @NotNull
        @DecimalMin("0.0")
        BigDecimal rentPrice,

        @DecimalMin("0.0")
        BigDecimal condominiumPrice,

        @NotNull
        Boolean available
) {
}
