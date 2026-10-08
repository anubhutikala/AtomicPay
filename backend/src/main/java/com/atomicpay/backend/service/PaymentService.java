package com.atomicpay.backend.service;

import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final PaymentValidator validator;

    public PaymentService() {
        this.validator = new PaymentValidator();
    }

    public PaymentResult processPayment(String senderId, String receiverId, double amount) {

        String error = validator.validate(senderId, receiverId, amount);

        if (error != null) {
            return new PaymentResult(false, error);
        }

        // Database payment processing will be connected here later.

        return new PaymentResult(true, "Payment request is valid");
    }
}