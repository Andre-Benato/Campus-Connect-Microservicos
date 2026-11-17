package com.campusconnect.apartment.infrastructure.adapters.out.persistence;

import com.campusconnect.apartment.infrastructure.adapters.in.web.dto.ApartmentFilter;
import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;

import com.campusconnect.apartment.domain.Apartment;
public final class ApartmentSpecifications {

    private ApartmentSpecifications() {}

    public static Specification<Apartment> from(ApartmentFilter filter) {
        return Specification.allOf(
                cityContains(filter.city()),
                neighborhoodContains(filter.neighborhood()),
                rentAtLeast(filter.minRent()),
                rentAtMost(filter.maxRent()),
                bedroomsEquals(filter.bedrooms()),
                availableEquals(filter.available()));
    }

    private static Specification<Apartment> cityContains(String city) {
        return (root, query, cb) -> {
            if (isBlank(city)) {
                return null;
            }
            return cb.like(cb.lower(root.get("city")), "%" + city.trim().toLowerCase() + "%");
        };
    }

    private static Specification<Apartment> neighborhoodContains(String neighborhood) {
        return (root, query, cb) -> {
            if (isBlank(neighborhood)) {
                return null;
            }
            return cb.like(
                    cb.lower(root.get("neighborhood")),
                    "%" + neighborhood.trim().toLowerCase() + "%");
        };
    }

    private static Specification<Apartment> rentAtLeast(BigDecimal minRent) {
        return (root, query, cb) -> {
            if (minRent == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("rentPrice"), minRent);
        };
    }

    private static Specification<Apartment> rentAtMost(BigDecimal maxRent) {
        return (root, query, cb) -> {
            if (maxRent == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("rentPrice"), maxRent);
        };
    }

    private static Specification<Apartment> bedroomsEquals(Integer bedrooms) {
        return (root, query, cb) -> {
            if (bedrooms == null) {
                return null;
            }
            return cb.equal(root.get("bedrooms"), bedrooms);
        };
    }

    private static Specification<Apartment> availableEquals(Boolean available) {
        return (root, query, cb) -> {
            if (available == null) {
                return null;
            }
            return cb.equal(root.get("available"), available);
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
