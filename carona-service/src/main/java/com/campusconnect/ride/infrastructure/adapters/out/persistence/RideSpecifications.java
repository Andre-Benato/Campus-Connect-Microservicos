package com.campusconnect.ride.infrastructure.adapters.out.persistence;

import com.campusconnect.ride.infrastructure.adapters.in.web.dto.RideFilter;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

import com.campusconnect.ride.domain.Ride;
import com.campusconnect.ride.domain.RideStatus;
public final class RideSpecifications {

    private RideSpecifications() {}

    public static Specification<Ride> from(RideFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.origin() != null && !filter.origin().isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("origin")),
                                "%" + filter.origin().trim().toLowerCase() + "%"));
            }
            if (filter.destination() != null && !filter.destination().isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("destination")),
                                "%" + filter.destination().trim().toLowerCase() + "%"));
            }
            if (filter.departureFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("departureAt"), filter.departureFrom()));
            }
            if (filter.departureTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("departureAt"), filter.departureTo()));
            }
            if (Boolean.TRUE.equals(filter.onlyAvailable())) {
                predicates.add(cb.equal(root.get("status"), RideStatus.OPEN));
                predicates.add(cb.greaterThan(root.get("availableSeats"), 0));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
