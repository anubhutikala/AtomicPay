package com.atomicpay.backend.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkerPool {

    private final ExecutorService executor;
    private final AccountLockManager lockManager;

    public WorkerPool(int numberOfWorkers) {
        executor = Executors.newFixedThreadPool(numberOfWorkers);
        lockManager = new AccountLockManager();
    }

    public void submitPayment(String senderId, String receiverId, double amount) {

        PaymentTask task = new PaymentTask(
            senderId,
            receiverId,
            amount,
            lockManager
        );

        executor.submit(task);
    }

    public void shutdown() {
        executor.shutdown();
    }
}