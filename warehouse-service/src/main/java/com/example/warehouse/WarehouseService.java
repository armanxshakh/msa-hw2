package com.example.warehouse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WarehouseService {

    private final StockRepository stockRepository;
    private final WarehouseReservationRepository reservationRepository;

    public WarehouseService(
            StockRepository stockRepository,
            WarehouseReservationRepository reservationRepository
    ) {
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public Stock setStock(Long productId, Integer quantity) {

        if (quantity == null || quantity < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity must be >= 0"
            );
        }

        Stock stock = stockRepository.findByProductId(productId)
                .orElseGet(() -> {
                    Stock newStock = new Stock();
                    newStock.setProductId(productId);
                    newStock.setReservedQuantity(0);
                    return newStock;
                });

        stock.setAvailableQuantity(quantity);

        return stockRepository.save(stock);
    }

    @Transactional
    public WarehouseReservation reserve(
            Long orderId,
            Long productId,
            Integer quantity
    ) {

        if (quantity == null || quantity <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity must be > 0"
            );
        }

        var existing = reservationRepository.findByOrderId(orderId);

        if (existing.isPresent()) {
            return existing.get();
        }

        Stock stock = stockRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found"
                        )
                );

        if (stock.getAvailableQuantity() < quantity) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Not enough stock"
            );
        }

        stock.setAvailableQuantity(
                stock.getAvailableQuantity() - quantity
        );

        stock.setReservedQuantity(
                stock.getReservedQuantity() + quantity
        );

        WarehouseReservation reservation = new WarehouseReservation();
        reservation.setOrderId(orderId);
        reservation.setProductId(productId);
        reservation.setQuantity(quantity);
        reservation.setStatus("RESERVED");

        stockRepository.save(stock);

        return reservationRepository.save(reservation);
    }

    @Transactional
    public WarehouseReservation release(Long orderId) {

        WarehouseReservation reservation =
                reservationRepository.findByOrderId(orderId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Reservation not found"
                                )
                        );

        if ("RELEASED".equals(reservation.getStatus())) {
            return reservation;
        }

        Stock stock =
                stockRepository.findByProductIdForUpdate(
                                reservation.getProductId()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Product not found"
                                )
                        );

        stock.setAvailableQuantity(
                stock.getAvailableQuantity()
                        + reservation.getQuantity()
        );

        stock.setReservedQuantity(
                stock.getReservedQuantity()
                        - reservation.getQuantity()
        );

        reservation.setStatus("RELEASED");

        stockRepository.save(stock);

        return reservationRepository.save(reservation);
    }
}