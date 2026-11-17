package com.campusconnect.apartment.infrastructure.adapters.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.campusconnect.apartment.domain.Apartment;
public interface ApartmentRepository
        extends JpaRepository<Apartment, Long>, JpaSpecificationExecutor<Apartment> {

    List<Apartment> findByUserId(Long userId);
}
