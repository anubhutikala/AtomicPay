package com.atomicpay.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atomicpay.backend.model.PaymentTransaction;
import com.atomicpay.backend.repository.PaymentTransactionRepository;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final PaymentTransactionRepository transactionRepository;

    public AdminController(
            PaymentTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentTransaction>> getAllPayments() {
        List<PaymentTransaction> transactions =
                transactionRepository.findAll();

        return ResponseEntity.ok(transactions);
    }
}