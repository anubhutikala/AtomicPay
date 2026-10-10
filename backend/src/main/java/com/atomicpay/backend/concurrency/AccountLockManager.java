package com.atomicpay.backend.concurrency;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class AccountLockManager {

    private final ConcurrentHashMap<String, ReentrantLock> accountLocks;

    public AccountLockManager() {
        accountLocks = new ConcurrentHashMap<>();
    }

    public ReentrantLock getLock(String accountId) {
        return accountLocks.computeIfAbsent(
            accountId,
            id -> new ReentrantLock()
        );
    }

    public void removeLock(String accountId) {
        accountLocks.remove(accountId);
    }
}