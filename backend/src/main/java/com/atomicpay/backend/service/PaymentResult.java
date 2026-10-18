package com.atomicpay.backend.service;

public class PaymentResult {

    private boolean success;
    private String message;
    private String transactionId;

    public PaymentResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.transactionId = null;
    }

    public PaymentResult(boolean success, String message, String transactionId) {
        this.success = success;
        this.message = message;
        this.transactionId = transactionId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getTransactionId() {
        return transactionId;
    }
}