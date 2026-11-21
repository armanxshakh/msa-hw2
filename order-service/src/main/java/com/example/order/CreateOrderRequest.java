package com.example.order;

import java.math.BigDecimal;

public record CreateOrderRequest(
        Long userId,
        BigDecimal price
) {
}