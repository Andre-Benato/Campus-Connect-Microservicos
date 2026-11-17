package com.campusconnect.apartment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "apartments")
public class Apartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "address", nullable = false, length = 200)
    private String address;

    @Column(name = "number", nullable = false, length = 20)
    private String number;

    @Column(name = "complement", length = 100)
    private String complement;

    @Column(name = "neighborhood", nullable = false, length = 100)
    private String neighborhood;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", nullable = false, length = 2)
    private String state;

    @Column(name = "cep", nullable = false, length = 9)
    private String cep;

    @Column(name = "bedrooms", nullable = false)
    private Integer bedrooms;

    @Column(name = "bathrooms", nullable = false)
    private Integer bathrooms;

    @Column(name = "rent_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal rentPrice;

    @Column(name = "condominium_price", precision = 10, scale = 2)
    private BigDecimal condominiumPrice;

    @Column(name = "available", nullable = false)
    private boolean available;

    /** Id do dono no user-service (referencia logica, sem FK entre bancos). */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    protected Apartment() {
        // exigido pelo JPA
    }

    public Apartment(
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
            Long userId) {

        this.name = name;
        this.description = description;
        this.address = address;
        this.number = number;
        this.complement = complement;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
        this.cep = cep;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.rentPrice = rentPrice;
        this.condominiumPrice = condominiumPrice;
        this.available = true;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getComplement() {
        return complement;
    }

    public void setComplement(String complement) {
        this.complement = complement;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public Integer getBedrooms() {
        return bedrooms;
    }

    public void setBedrooms(Integer bedrooms) {
        this.bedrooms = bedrooms;
    }

    public Integer getBathrooms() {
        return bathrooms;
    }

    public void setBathrooms(Integer bathrooms) {
        this.bathrooms = bathrooms;
    }

    public BigDecimal getRentPrice() {
        return rentPrice;
    }

    public void setRentPrice(BigDecimal rentPrice) {
        this.rentPrice = rentPrice;
    }

    public BigDecimal getCondominiumPrice() {
        return condominiumPrice;
    }

    public void setCondominiumPrice(BigDecimal condominiumPrice) {
        this.condominiumPrice = condominiumPrice;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Long getUserId() {
        return userId;
    }
}
