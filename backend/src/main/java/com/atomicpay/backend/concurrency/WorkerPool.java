package com.atomicpay.backend.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.stereotype.Component;

import com.atomicpay.backend.service.PaymentResult;
import com.atomicpay.backend.service.PaymentService;

@Component
public class WorkerPool {

    private final ExecutorService executor;
    private final PaymentService paymentService;

    public WorkerPool(PaymentService paymentService) {
        this.paymentService = paymentService;
        this.executor = Executors.newFixedThreadPool(3);
    }

   public Future<PaymentResult> submitPayment(
        String senderId,
        String receiverId,
        double amount) {

    PaymentTask task = new PaymentTask(
            senderId,
            receiverId,
            amount,
            paymentService
    );

    return executor.submit(task);
}

    public void shutdown() {
        executor.shutdown();
    }
}