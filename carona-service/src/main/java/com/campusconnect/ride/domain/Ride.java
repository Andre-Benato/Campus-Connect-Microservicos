package com.campusconnect.ride.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rides")
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Id do motorista no user-service (referencia logica, sem FK entre bancos). */
    @Column(name = "driver_user_id", nullable = false)
    private Long driverUserId;

    @Column(name = "origin", nullable = false, length = 200)
    private String origin;

    @Column(name = "destination", nullable = false, length = 200)
    private String destination;

    @Column(name = "departure_at", nullable = false)
    private LocalDateTime departureAt;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @Column(name = "price_per_seat", precision = 10, scale = 2)
    private BigDecimal pricePerSeat;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RideStatus status;

    protected Ride() {
        // exigido pelo JPA
    }

    public Ride(
            Long driverUserId,
            String origin,
            String destination,
            LocalDateTime departureAt,
            Integer totalSeats,
            BigDecimal pricePerSeat,
            String notes) {
        this.driverUserId = driverUserId;
        this.origin = origin;
        this.destination = destination;
        this.departureAt = departureAt;
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats;
        this.pricePerSeat = pricePerSeat;
        this.notes = notes;
        this.status = RideStatus.OPEN;
    }

    public Long getId() {
        return id;
    }

    public Long getDriverUserId() {
        return driverUserId;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public LocalDateTime getDepartureAt() {
        return departureAt;
    }

    public void setDepartureAt(LocalDateTime departureAt) {
        this.departureAt = departureAt;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
    }

    public BigDecimal getPricePerSeat() {
        return pricePerSeat;
    }

    public void setPricePerSeat(BigDecimal pricePerSeat) {
        this.pricePerSeat = pricePerSeat;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public RideStatus getStatus() {
        return status;
    }

    public void setStatus(RideStatus status) {
        this.status = status;
    }
}
