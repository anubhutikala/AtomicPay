package com.atomicpay.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class PaymentRequest {

    @NotBlank(message = "Sender account is required")
    private String senderId;

    @NotBlank(message = "Receiver account is required")
    private String receiverId;

    @Positive(message = "Payment amount must be greater than zero")
    private double amount;

    public PaymentRequest() {
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}