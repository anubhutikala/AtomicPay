package com.atomicpay.backend.recovery;

public class RecoveryTest {

    public static void main(String[] args) {

        RecoveryService service = new RecoveryService(null);

        RecoveryRecord record = new RecoveryRecord("ORD001");

        System.out.println("Initial State: " + record.getState());

        service.markProcessing(record);
        System.out.println("After Processing: " + record.getState());

        service.markIncomplete(record, "Worker stopped unexpectedly");
        System.out.println("After Failure: " + record.getState());

        service.recover(record);
        System.out.println("After Recovery: " + record.getState());

        System.out.println("Message: " + record.getMessage());
    }
}