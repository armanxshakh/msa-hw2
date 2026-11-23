package com.example.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WarehouseReservationRepository
        extends JpaRepository<WarehouseReservation, Long> {

    Optional<WarehouseReservation> findByOrderId(Long orderId);
}