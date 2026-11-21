package com.example.notification;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class PaymentResultListener {

    private final NotificationRepository repository;
    private final ObjectMapper objectMapper;

    public PaymentResultListener(
            NotificationRepository repository,
            ObjectMapper objectMapper) {

        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "payment-result")
    public void handle(String payload) throws JacksonException {

        PaymentResultEvent event =
                objectMapper.readValue(payload, PaymentResultEvent.class);

        Notification notification = new Notification();

        notification.setUserId(event.userId());
        notification.setOrderId(event.orderId());
        notification.setEmail(event.email());

        if (event.success()) {
            notification.setSubject("Письмо счастья");
            notification.setMessage(
                    "Заказ успешно оплачен. Сумма: " + event.amount()
            );
        } else {
            notification.setSubject("Письмо горя");
            notification.setMessage(
                    "Недостаточно средств для оплаты заказа. Сумма: "
                            + event.amount()
            );
        }

        repository.save(notification);
    }
}