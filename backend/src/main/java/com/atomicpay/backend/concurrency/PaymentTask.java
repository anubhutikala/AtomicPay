package com.atomicpay.backend.concurrency;

import java.util.concurrent.Callable;

import com.atomicpay.backend.service.PaymentResult;
import com.atomicpay.backend.service.PaymentService;

public class PaymentTask implements Callable<PaymentResult> {

    private final String senderId;
    private final String receiverId;
    private final double amount;
    private final PaymentService paymentService;

    public PaymentTask(
            String senderId,
            String receiverId,
            double amount,
            PaymentService paymentService) {

        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.paymentService = paymentService;
    }

    @Override
    public PaymentResult call() {

        String workerName = Thread.currentThread().getName();

        System.out.println(
                workerName + " processing payment: "
                + senderId + " -> " + receiverId
                + " | Amount: " + amount
        );

        PaymentResult result = paymentService.processPayment(
                senderId,
                receiverId,
                amount,
                workerName
        );

        if (result.isSuccess()) {
            System.out.println(
                    workerName + " payment successful. Transaction ID: "
                    + result.getTransactionId()
            );
        } else {
            System.out.println(
                    workerName + " payment failed: "
                    + result.getMessage()
            );
        }

        return result;
    }
}