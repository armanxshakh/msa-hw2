package com.example.billing;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {

    private final BillingAccountRepository repository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, PaymentResultEvent> kafkaTemplate;

    public OrderCreatedListener(
            BillingAccountRepository repository,
            ObjectMapper objectMapper,
            KafkaTemplate<String, PaymentResultEvent> kafkaTemplate) {

        this.repository = repository;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "order-created")
    public void handle(String payload) throws JacksonException {

        OrderCreatedEvent event =
                objectMapper.readValue(payload, OrderCreatedEvent.class);

        BillingAccount account = repository
                .findByUserId(event.userId())
                .orElseThrow(() ->
                        new IllegalStateException("Billing account not found")
                );

        boolean success;

        if (account.getBalance().compareTo(event.price()) >= 0) {

            account.setBalance(
                    account.getBalance().subtract(event.price())
            );

            repository.save(account);

            success = true;

        } else {
            success = false;
        }

        PaymentResultEvent result = new PaymentResultEvent(
                event.orderId(),
                event.userId(),
                account.getEmail(),
                event.price(),
                success,
                success
                        ? "Payment successful"
                        : "Insufficient funds"
        );

        kafkaTemplate.send(
                "payment-result",
                event.orderId().toString(),
                result
        );
    }
}