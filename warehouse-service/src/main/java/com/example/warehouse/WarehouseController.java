package com.example.warehouse;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/warehouse")
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final StockRepository stockRepository;

    public WarehouseController(
            WarehouseService warehouseService,
            StockRepository stockRepository
    ) {
        this.warehouseService = warehouseService;
        this.stockRepository = stockRepository;
    }

    @PutMapping("/stock/{productId}")
    public Stock setStock(
            @PathVariable Long productId,
            @RequestBody StockRequest request
    ) {
        return warehouseService.setStock(
                productId,
                request.quantity()
        );
    }

    @GetMapping("/stock/{productId}")
    public Stock getStock(@PathVariable Long productId) {
        return stockRepository.findByProductId(productId)
                .orElseThrow();
    }

    @PostMapping("/reservations")
    public WarehouseReservation reserve(
            @RequestBody ReserveRequest request
    ) {
        return warehouseService.reserve(
                request.orderId(),
                request.productId(),
                request.quantity()
        );
    }

    @PostMapping("/reservations/{orderId}/release")
    public WarehouseReservation release(
            @PathVariable Long orderId
    ) {
        return warehouseService.release(orderId);
    }

    public record StockRequest(Integer quantity) {}

    public record ReserveRequest(
            Long orderId,
            Long productId,
            Integer quantity
    ) {}
}