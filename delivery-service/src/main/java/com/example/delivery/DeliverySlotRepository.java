package com.example.delivery;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DeliverySlotRepository extends JpaRepository<DeliverySlot, Long> {

    Optional<DeliverySlot> findBySlot(String slot);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DeliverySlot d where d.slot = :slot")
    Optional<DeliverySlot> findBySlotForUpdate(@Param("slot") String slot);
}