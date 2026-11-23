package com.example.delivery;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DeliveryService {

    private final DeliverySlotRepository slotRepository;
    private final DeliveryReservationRepository reservationRepository;

    public DeliveryService(
            DeliverySlotRepository slotRepository,
            DeliveryReservationRepository reservationRepository
    ) {
        this.slotRepository = slotRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public DeliverySlot setSlot(String slot, Integer couriers) {

        if (couriers == null || couriers < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Couriers must be >= 0"
            );
        }

        DeliverySlot deliverySlot = slotRepository.findBySlot(slot)
                .orElseGet(() -> {
                    DeliverySlot newSlot = new DeliverySlot();
                    newSlot.setSlot(slot);
                    newSlot.setReservedCouriers(0);
                    return newSlot;
                });

        deliverySlot.setAvailableCouriers(couriers);

        return slotRepository.save(deliverySlot);
    }

    @Transactional
    public DeliveryReservation reserve(Long orderId, String slot) {

        var existing = reservationRepository.findByOrderId(orderId);

        if (existing.isPresent()) {
            return existing.get();
        }

        DeliverySlot deliverySlot = slotRepository.findBySlotForUpdate(slot)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Delivery slot not found"
                        )
                );

        if (deliverySlot.getAvailableCouriers() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No couriers available"
            );
        }

        deliverySlot.setAvailableCouriers(
                deliverySlot.getAvailableCouriers() - 1
        );

        deliverySlot.setReservedCouriers(
                deliverySlot.getReservedCouriers() + 1
        );

        DeliveryReservation reservation = new DeliveryReservation();
        reservation.setOrderId(orderId);
        reservation.setSlot(slot);
        reservation.setStatus("RESERVED");

        slotRepository.save(deliverySlot);

        return reservationRepository.save(reservation);
    }

    @Transactional
    public DeliveryReservation release(Long orderId) {

        DeliveryReservation reservation =
                reservationRepository.findByOrderId(orderId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Delivery reservation not found"
                                )
                        );

        if ("RELEASED".equals(reservation.getStatus())) {
            return reservation;
        }

        DeliverySlot deliverySlot =
                slotRepository.findBySlotForUpdate(reservation.getSlot())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Delivery slot not found"
                                )
                        );

        deliverySlot.setAvailableCouriers(
                deliverySlot.getAvailableCouriers() + 1
        );

        deliverySlot.setReservedCouriers(
                deliverySlot.getReservedCouriers() - 1
        );

        reservation.setStatus("RELEASED");

        slotRepository.save(deliverySlot);

        return reservationRepository.save(reservation);
    }
}