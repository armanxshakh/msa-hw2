package com.example.msahw2;

public record ProfileResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        String email,
        String phone
) {
}