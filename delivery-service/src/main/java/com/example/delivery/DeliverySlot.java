package com.example.delivery;

import jakarta.persistence.*;

@Entity
@Table(name = "delivery_slots")
public class DeliverySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slot;

    @Column(nullable = false)
    private Integer availableCouriers = 0;

    @Column(nullable = false)
    private Integer reservedCouriers = 0;

    public Long getId() {
        return id;
    }

    public String getSlot() {
        return slot;
    }

    public void setSlot(String slot) {
        this.slot = slot;
    }

    public Integer getAvailableCouriers() {
        return availableCouriers;
    }

    public void setAvailableCouriers(Integer availableCouriers) {
        this.availableCouriers = availableCouriers;
    }

    public Integer getReservedCouriers() {
        return reservedCouriers;
    }

    public void setReservedCouriers(Integer reservedCouriers) {
        this.reservedCouriers = reservedCouriers;
    }
}