package com.atomicpay.backend.concurrency;

public class ConcurrencyTest {

    public static void main(String[] args) {

        WorkerPool pool = new WorkerPool(3);

        // Two payments using the same sender account
        pool.submitPayment("A001", "A002", 500);
        pool.submitPayment("A001", "A003", 700);

        // Another independent payment
        pool.submitPayment("A004", "A005", 300);

        pool.shutdown();
    }
}