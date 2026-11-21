package com.example.billing;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class UserCreatedListener {

    private final BillingAccountRepository repository;
    private final ObjectMapper objectMapper;

    public UserCreatedListener(
            BillingAccountRepository repository,
            ObjectMapper objectMapper) {

        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "user-created")
    public void handle(String payload) throws JacksonException {

        UserCreatedEvent event =
                objectMapper.readValue(payload, UserCreatedEvent.class);

        if (repository.existsByUserId(event.userId())) {
            return;
        }

        BillingAccount account = new BillingAccount();

        account.setUserId(event.userId());
        account.setEmail(event.email());
        account.setBalance(BigDecimal.ZERO);

        repository.save(account);

        System.out.println(
                "Billing account created for userId=" + event.userId()
        );
    }
}