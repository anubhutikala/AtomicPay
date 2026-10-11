package com.atomicpay.backend.recovery;

public class RecoveryRecord {

    private String orderId;
    private TransactionState state;
    private String message;

    public RecoveryRecord(String orderId) {
        this.orderId = orderId;
        this.state = TransactionState.PENDING;
        this.message = "Transaction created";
    }

    public String getOrderId() {
        return orderId;
    }

    public TransactionState getState() {
        return state;
    }

    public String getMessage() {
        return message;
    }

    public void setState(TransactionState state) {
        this.state = state;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}