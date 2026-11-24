package com.atomicpay.backend.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class WorkerMonitor {

    private final AtomicInteger activeWorkers = new AtomicInteger(0);

    private final AtomicLong totalAttempts = new AtomicLong(0);
    private final AtomicLong successfulAttempts = new AtomicLong(0);
    private final AtomicLong failedAttempts = new AtomicLong(0);

    private final ConcurrentHashMap<String, AtomicLong> workerTasks =
            new ConcurrentHashMap<>();

    public void paymentStarted(String workerName) {
        activeWorkers.incrementAndGet();
        totalAttempts.incrementAndGet();

        workerTasks.computeIfAbsent(
                workerName, k -> new AtomicLong(0)
        ).incrementAndGet();
    }

    public void paymentSucceeded() {
        successfulAttempts.incrementAndGet();
    }

    public void paymentFailed() {
        failedAttempts.incrementAndGet();
    }

    public void paymentFinished() {
        activeWorkers.decrementAndGet();
    }

    public int getActiveWorkers() {
        return activeWorkers.get();
    }

    public long getTotalAttempts() {
        return totalAttempts.get();
    }

    public long getSuccessfulAttempts() {
        return successfulAttempts.get();
    }

    public long getFailedAttempts() {
        return failedAttempts.get();
    }

    public ConcurrentHashMap<String, AtomicLong> getWorkerTasks() {
        return workerTasks;
    }
}