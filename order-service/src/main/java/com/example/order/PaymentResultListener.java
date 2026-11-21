package com.example.order;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class PaymentResultListener {

    private final OrderRepository repository;
    private final ObjectMapper objectMapper;

    public PaymentResultListener(
            OrderRepository repository,
            ObjectMapper objectMapper) {

        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment-result")
    public void handle(String payload) throws JacksonException {

        PaymentResultEvent event =
                objectMapper.readValue(payload, PaymentResultEvent.class);

        repository.findById(event.orderId())
                .ifPresent(order -> {

                    order.setStatus(
                            event.success() ? "PAID" : "FAILED"
                    );

                    repository.save(order);
                });
    }
}