package com.example.msahw2;

public record UserCreatedEvent(
        Long userId,
        String username,
        String email
) {}