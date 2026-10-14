package com.atomicpay.backend.controller;

import com.atomicpay.backend.dto.PaymentRequest;
import com.atomicpay.backend.service.PaymentResult;
import com.atomicpay.backend.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/test")
    public String test() {
        return "Payment Controller is working";
    }

    @PostMapping
    public ResponseEntity<PaymentResult> processPayment(
            @Valid @RequestBody PaymentRequest request) {

        if (request == null) {
            return ResponseEntity.badRequest()
                    .body(new PaymentResult(false,
                            "Payment request is required"));
        }

        PaymentResult result = paymentService.processPayment(
                request.getSenderId(),
                request.getReceiverId(),
                request.getAmount()
        );

        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }
}