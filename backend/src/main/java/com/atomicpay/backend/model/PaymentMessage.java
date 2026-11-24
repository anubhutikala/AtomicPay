package com.atomicpay.backend.model;

import java.io.Serializable;

public class PaymentMessage implements Serializable {

    private String orderId;
    private String senderId;
    private String receiverId;
    private double amount;

    public PaymentMessage() {
    }

    public PaymentMessage(
            String orderId,
            String senderId,
            String receiverId,
            double amount) {

        this.orderId = orderId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
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