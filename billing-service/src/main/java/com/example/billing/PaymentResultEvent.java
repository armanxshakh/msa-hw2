package com.example.billing;

import java.math.BigDecimal;

public record PaymentResultEvent(
        Long orderId,
        Long userId,
        String email,
        BigDecimal amount,
        boolean success,
        String message
) {
}