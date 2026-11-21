package com.example.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingAccountRepository
        extends JpaRepository<BillingAccount, Long> {

    Optional<BillingAccount> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}