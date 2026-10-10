package com.atomicpay.backend.concurrency;

import java.util.concurrent.locks.ReentrantLock;

public class PaymentTask implements Runnable {

    private String senderId;
    private String receiverId;
    private double amount;
    private AccountLockManager lockManager;

    public PaymentTask(
            String senderId,
            String receiverId,
            double amount,
            AccountLockManager lockManager) {

        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.lockManager = lockManager;
    }

    @Override
    public void run() {

        String workerName = Thread.currentThread().getName();

        ReentrantLock senderLock = lockManager.getLock(senderId);
        ReentrantLock receiverLock = lockManager.getLock(receiverId);

        // Lock accounts in a fixed order
        ReentrantLock firstLock;
        ReentrantLock secondLock;

        if (senderId.compareTo(receiverId) < 0) {
            firstLock = senderLock;
            secondLock = receiverLock;
        } else {
            firstLock = receiverLock;
            secondLock = senderLock;
        }

        firstLock.lock();

        try {
            secondLock.lock();

            try {
                System.out.println(
                    workerName + " processing payment: "
                    + senderId + " -> "
                    + receiverId
                    + " | Amount: " + amount
                );

                Thread.sleep(1000);

                System.out.println(
                    workerName + " finished payment."
                );

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println(workerName + " was interrupted.");

            } finally {
                secondLock.unlock();
            }

        } finally {
            firstLock.unlock();
        }
    }
}