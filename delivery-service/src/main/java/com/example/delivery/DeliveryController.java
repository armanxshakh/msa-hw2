package com.example.delivery;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/delivery")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final DeliverySlotRepository slotRepository;

    public DeliveryController(
            DeliveryService deliveryService,
            DeliverySlotRepository slotRepository
    ) {
        this.deliveryService = deliveryService;
        this.slotRepository = slotRepository;
    }

    @PutMapping("/slots/{slot}")
    public DeliverySlot setSlot(
            @PathVariable String slot,
            @RequestBody SlotRequest request
    ) {
        return deliveryService.setSlot(
                slot,
                request.couriers()
        );
    }

    @GetMapping("/slots/{slot}")
    public DeliverySlot getSlot(@PathVariable String slot) {
        return slotRepository.findBySlot(slot)
                .orElseThrow();
    }

    @PostMapping("/reservations")
    public DeliveryReservation reserve(
            @RequestBody ReserveRequest request
    ) {
        return deliveryService.reserve(
                request.orderId(),
                request.slot()
        );
    }

    @PostMapping("/reservations/{orderId}/release")
    public DeliveryReservation release(
            @PathVariable Long orderId
    ) {
        return deliveryService.release(orderId);
    }

    public record SlotRequest(Integer couriers) {}
    public record ReserveRequest(
            Long orderId,
            String slot
    ) {}
}