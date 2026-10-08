package com.atomicpay.backend.service;

public class PaymentValidator {

    public String validate(String senderId, String receiverId, double amount) {

        if (senderId == null || senderId.isBlank()) {
            return "Sender account is required";
        }

        if (receiverId == null || receiverId.isBlank()) {
            return "Receiver account is required";
        }

        if (senderId.equals(receiverId)) {
            return "Sender and receiver cannot be the same";
        }

        if (amount <= 0) {
            return "Payment amount must be greater than zero";
        }

        return null;
    }
}