package com.example.billing;

public record UserCreatedEvent(
        Long userId,
        String username,
        String email
) {
}