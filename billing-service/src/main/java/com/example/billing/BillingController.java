package com.example.billing;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/billing")
public class BillingController {

    private final BillingAccountRepository repository;

    public BillingController(BillingAccountRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getAccount(@PathVariable Long userId) {
        return repository.findByUserId(userId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{userId}/deposit")
    public ResponseEntity<?> deposit(
            @PathVariable Long userId,
            @RequestBody Map<String, BigDecimal> body) {

        BigDecimal amount = body.get("amount");

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Amount must be positive"));
        }

        return repository.findByUserId(userId)
                .<ResponseEntity<?>>map(account -> {
                    account.setBalance(
                            account.getBalance().add(amount)
                    );

                    return ResponseEntity.ok(repository.save(account));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{userId}/withdraw")
    public ResponseEntity<?> withdraw(
            @PathVariable Long userId,
            @RequestBody Map<String, BigDecimal> body) {

        BigDecimal amount = body.get("amount");

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Amount must be positive"));
        }

        return repository.findByUserId(userId)
                .<ResponseEntity<?>>map(account -> {

                    if (account.getBalance().compareTo(amount) < 0) {
                        return ResponseEntity
                                .badRequest()
                                .body(Map.of(
                                        "error",
                                        "Insufficient funds"
                                ));
                    }

                    account.setBalance(
                            account.getBalance().subtract(amount)
                    );

                    return ResponseEntity.ok(repository.save(account));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}