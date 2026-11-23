package com.example.billing;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@RestController
@RequestMapping("/billing/payments")
public class SagaBillingController {

    private final BillingAccountRepository accountRepository;
    private final BillingTransactionRepository transactionRepository;

    public SagaBillingController(
            BillingAccountRepository accountRepository,
            BillingTransactionRepository transactionRepository
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping
    @Transactional
    public BillingTransaction pay(
            @RequestBody PaymentRequest request
    ) {

        if (request.amount() == null ||
                request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Amount must be > 0"
            );
        }

        var existing =
                transactionRepository.findByOrderId(request.orderId());

        if (existing.isPresent()) {
            return existing.get();
        }

        BillingAccount account =
                accountRepository.findByUserId(request.userId())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Billing account not found"
                                )
                        );

        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient funds"
            );
        }

        account.setBalance(
                account.getBalance().subtract(request.amount())
        );

        BillingTransaction transaction =
                new BillingTransaction();

        transaction.setOrderId(request.orderId());
        transaction.setUserId(request.userId());
        transaction.setAmount(request.amount());
        transaction.setStatus("PAID");

        accountRepository.save(account);

        return transactionRepository.save(transaction);
    }

    @PostMapping("/{orderId}/refund")
    @Transactional
    public BillingTransaction refund(
            @PathVariable Long orderId
    ) {

        BillingTransaction transaction =
                transactionRepository.findByOrderId(orderId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Payment not found"
                                )
                        );

        if ("REFUNDED".equals(transaction.getStatus())) {
            return transaction;
        }

        BillingAccount account =
                accountRepository.findByUserId(
                                transaction.getUserId()
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Billing account not found"
                                )
                        );

        account.setBalance(
                account.getBalance().add(
                        transaction.getAmount()
                )
        );

        transaction.setStatus("REFUNDED");

        accountRepository.save(account);

        return transactionRepository.save(transaction);
    }

    public record PaymentRequest(
            Long orderId,
            Long userId,
            BigDecimal amount
    ) {}
}