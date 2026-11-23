package com.example.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SagaOrderRepository
        extends JpaRepository<SagaOrder, Long> {
}
