package com.example.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingTransactionRepository
        extends JpaRepository<BillingTransaction, Long> {

    Optional<BillingTransaction> findByOrderId(Long orderId);
}