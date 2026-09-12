package com.example.msahw2;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping({"/health", "/health/"})
    public Map<String, String> health() {
        return Map.of("status", "OK");
    }
}