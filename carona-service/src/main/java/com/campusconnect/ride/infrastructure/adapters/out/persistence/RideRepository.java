package com.campusconnect.ride.infrastructure.adapters.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.campusconnect.ride.domain.Ride;
public interface RideRepository extends JpaRepository<Ride, Long>, JpaSpecificationExecutor<Ride> {

    List<Ride> findByDriverUserIdOrderByDepartureAtAsc(Long driverUserId);
}
