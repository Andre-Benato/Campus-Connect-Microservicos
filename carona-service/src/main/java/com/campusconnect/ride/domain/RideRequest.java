package com.campusconnect.ride.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ride_requests")
public class RideRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ride_id", nullable = false)
    private Long rideId;

    /** Id do passageiro no user-service (referencia logica, sem FK entre bancos). */
    @Column(name = "passenger_user_id", nullable = false)
    private Long passengerUserId;

    @Column(name = "seats_requested", nullable = false)
    private Integer seatsRequested;

    @Column(name = "message", length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RideRequestStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected RideRequest() {
        // exigido pelo JPA
    }

    public RideRequest(Long rideId, Long passengerUserId, Integer seatsRequested, String message) {
        this.rideId = rideId;
        this.passengerUserId = passengerUserId;
        this.seatsRequested = seatsRequested;
        this.message = message;
        this.status = RideRequestStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getRideId() {
        return rideId;
    }

    public Long getPassengerUserId() {
        return passengerUserId;
    }

    public Integer getSeatsRequested() {
        return seatsRequested;
    }

    public String getMessage() {
        return message;
    }

    public RideRequestStatus getStatus() {
        return status;
    }

    public void setStatus(RideRequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
