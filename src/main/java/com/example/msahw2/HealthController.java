package com.example.msahw2;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health/")
    public HealthResponse health() {
        return new HealthResponse("OK");
    }
}