package com.campusconnect.ride.infrastructure.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.campusconnect.ride.domain.RideRequest;
import com.campusconnect.ride.domain.RideRequestStatus;
public interface RideRequestRepository extends JpaRepository<RideRequest, Long> {

    List<RideRequest> findByRideIdOrderByCreatedAtAsc(Long rideId);

    List<RideRequest> findByPassengerUserIdOrderByCreatedAtDesc(Long passengerUserId);

    List<RideRequest> findByRideIdAndStatusIn(Long rideId, List<RideRequestStatus> statuses);

    boolean existsByRideIdAndPassengerUserIdAndStatusIn(
            Long rideId, Long passengerUserId, List<RideRequestStatus> statuses);

    Optional<RideRequest> findByIdAndRideId(Long id, Long rideId);
}
