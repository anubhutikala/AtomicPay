package com.atomicpay.backend.concurrency;

import java.util.concurrent.Future;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.atomicpay.backend.BackendApplication;
import com.atomicpay.backend.service.PaymentResult;

public class ConcurrencyTest {

    public static void main(String[] args) throws Exception {

        ConfigurableApplicationContext context =
                SpringApplication.run(BackendApplication.class, args);

        try {
            WorkerPool pool = context.getBean(WorkerPool.class);

            Future<PaymentResult> p1 =
                    pool.submitPayment("A001", "A002", 500);

            Future<PaymentResult> p2 =
                    pool.submitPayment("A001", "A003", 700);

            Future<PaymentResult> p3 =
                    pool.submitPayment("A004", "A005", 300);

            System.out.println("\n--- CONCURRENCY TEST RESULTS ---");

            System.out.println("Payment 1: " + p1.get().getMessage());
            System.out.println("Payment 2: " + p2.get().getMessage());
            System.out.println("Payment 3: " + p3.get().getMessage());

            System.out.println("\nAll payment tasks have finished.");

            pool.shutdown();

        } finally {
            context.close();
        }
    }
}