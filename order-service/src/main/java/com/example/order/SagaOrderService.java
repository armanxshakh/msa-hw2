package com.example.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class SagaOrderService {

    private final SagaOrderRepository repository;
    private final RestClient restClient;

    @Value("${services.billing-url}")
    private String billingUrl;

    @Value("${services.warehouse-url}")
    private String warehouseUrl;

    @Value("${services.delivery-url}")
    private String deliveryUrl;



    public SagaOrderService(
            SagaOrderRepository repository
    ) {
        this.repository = repository;
        this.restClient = RestClient.create();
    }

    public SagaOrder createOrder(
            Long userId,
            Long productId,
            Integer quantity,
            BigDecimal price,
            String deliverySlot
    ) {

        SagaOrder order = new SagaOrder();
        order.setUserId(userId);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setPrice(price);
        order.setDeliverySlot(deliverySlot);
        order.setStatus("PROCESSING");

        order = repository.save(order);

        boolean paymentDone = false;
        boolean warehouseReserved = false;
        boolean deliveryReserved = false;

        try {

            // STEP 1: Billing
            restClient.post()
                    .uri(billingUrl + "/billing/payments")
                    .body(Map.of(
                            "orderId", order.getId(),
                            "userId", userId,
                            "amount", price
                    ))
                    .retrieve()
                    .toBodilessEntity();

            paymentDone = true;

            // STEP 2: Warehouse
            restClient.post()
                    .uri(warehouseUrl + "/warehouse/reservations")
                    .body(Map.of(
                            "orderId", order.getId(),
                            "productId", productId,
                            "quantity", quantity
                    ))
                    .retrieve()
                    .toBodilessEntity();

            warehouseReserved = true;

            // STEP 3: Delivery
            restClient.post()
                    .uri(deliveryUrl + "/delivery/reservations")
                    .body(Map.of(
                            "orderId", order.getId(),
                            "slot", deliverySlot
                    ))
                    .retrieve()
                    .toBodilessEntity();

            deliveryReserved = true;

            order.setStatus("COMPLETED");
            order.setFailureReason(null);

            return repository.save(order);

        } catch (Exception exception) {

            // Compensation выполняем в обратном порядке.

            if (deliveryReserved) {
                try {
                    restClient.post()
                            .uri(deliveryUrl
                                    + "/delivery/reservations/"
                                    + order.getId()
                                    + "/release")
                            .retrieve()
                            .toBodilessEntity();
                } catch (Exception ignored) {
                }
            }

            if (warehouseReserved) {
                try {
                    restClient.post()
                            .uri(warehouseUrl
                                    + "/warehouse/reservations/"
                                    + order.getId()
                                    + "/release")
                            .retrieve()
                            .toBodilessEntity();
                } catch (Exception ignored) {
                }
            }

            if (paymentDone) {
                try {
                    restClient.post()
                            .uri(billingUrl
                                    + "/billing/payments/"
                                    + order.getId()
                                    + "/refund")
                            .retrieve()
                            .toBodilessEntity();
                } catch (Exception ignored) {
                }
            }

            order.setStatus("FAILED");
            order.setFailureReason(
                    exception.getMessage() != null
                            ? exception.getMessage()
                            : "Saga failed"
            );

            return repository.save(order);
        }
    }
}