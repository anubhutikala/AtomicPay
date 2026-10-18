package com.atomicpay.backend.controller;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atomicpay.backend.concurrency.WorkerPool;
import com.atomicpay.backend.dto.PaymentRequest;
import com.atomicpay.backend.model.PaymentTransaction;
import com.atomicpay.backend.recovery.RecoveryService;
import com.atomicpay.backend.repository.PaymentTransactionRepository;
import com.atomicpay.backend.service.PaymentResult;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = {
        "http://localhost:5500",
        "http://127.0.0.1:5500"
})
public class PaymentController {

    private final WorkerPool workerPool;
    private final PaymentTransactionRepository transactionRepository;
    private final RecoveryService recoveryService;

    public PaymentController(
            WorkerPool workerPool,
            PaymentTransactionRepository transactionRepository,
            RecoveryService recoveryService) {

        this.workerPool = workerPool;
        this.transactionRepository = transactionRepository;
        this.recoveryService = recoveryService;
    }

    // Test whether the payment API is working
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Payment API is working!");
    }

    // Retrieve transaction history
    @GetMapping("/history")
    public ResponseEntity<List<PaymentTransaction>> getTransactionHistory() {
        return ResponseEntity.ok(transactionRepository.findAll());
    }

    // Process a payment using the worker pool
    @PostMapping
    public ResponseEntity<?> makePayment(
            @Valid @RequestBody PaymentRequest request) {

        if (request == null) {
            return ResponseEntity.badRequest()
                    .body("Payment request cannot be empty.");
        }

        try {
            Future<PaymentResult> future = workerPool.submitPayment(
                    request.getSenderId(),
                    request.getReceiverId(),
                    request.getAmount()
            );

            PaymentResult result = future.get(10, TimeUnit.SECONDS);

            if (!result.isSuccess()) {
                return ResponseEntity.badRequest().body(result);
            }

            return ResponseEntity.ok(result);

        } catch (TimeoutException e) {
            return ResponseEntity.status(503)
                    .body("Payment processing timed out. Check transaction history before retrying.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            return ResponseEntity.status(503)
                    .body("Payment processing was interrupted.");

        } catch (ExecutionException e) {
            return ResponseEntity.internalServerError()
                    .body("Payment processing failed due to a server error.");
        }
    }

    // Recover verified stuck sample transactions
    @PostMapping("/recovery")
    public ResponseEntity<String> recoverTransactions() {
        int recoveredCount = recoveryService.recoverStuckTransactions();

        return ResponseEntity.ok(
                "Recovery completed. Transactions marked as
