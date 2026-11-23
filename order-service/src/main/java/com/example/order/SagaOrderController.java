package com.example.order;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/orders/saga")
public class SagaOrderController {

    private final SagaOrderService sagaOrderService;
    private final SagaOrderRepository repository;

    public SagaOrderController(
            SagaOrderService sagaOrderService,
            SagaOrderRepository repository
    ) {
        this.sagaOrderService = sagaOrderService;
        this.repository = repository;
    }

    @PostMapping
    public SagaOrder create(
            @RequestBody CreateSagaOrderRequest request
    ) {
        return sagaOrderService.createOrder(
                request.userId(),
                request.productId(),
                request.quantity(),
                request.price(),
                request.deliverySlot()
        );
    }

    @GetMapping("/{orderId}")
    public SagaOrder get(@PathVariable Long orderId) {
        return repository.findById(orderId)
                .orElseThrow();
    }

    public record CreateSagaOrderRequest(
            Long userId,
            Long productId,
            Integer quantity,
            BigDecimal price,
            String deliverySlot
    ) {}
}