package com.example.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryReservationRepository
        extends JpaRepository<DeliveryReservation, Long> {

    Optional<DeliveryReservation> findByOrderId(Long orderId);
}