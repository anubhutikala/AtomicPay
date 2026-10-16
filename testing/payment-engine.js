
"use strict";

class PaymentEngine {
    constructor(initialBalances = {}) {
        this.balances = new Map(Object.entries(initialBalances));
        this.transactions = [];
        this.processedRequests = new Map();
        this.nextTransactionId = 1;
        this.accountLocks = new Map();
    }

    getBalance(accountId) {
        if (!this.balances.has(accountId)) {
            throw new Error(`Account not found: ${accountId}`);
        }

        return this.balances.get(accountId);
    }

    getTransactions() {
        return this.transactions.map(transaction => ({ ...transaction }));
    }

    async processPayment({
        senderAccountId,
        receiverAccountId,
        amount,
        requestId,
        simulateFailure = false
    }) {
        if (!senderAccountId || !receiverAccountId) {
            throw new Error("Sender and receiver account IDs are required.");
        }

        if (senderAccountId === receiverAccountId) {
            throw new Error("Sender and receiver accounts must be different.");
        }

        if (!Number.isFinite(amount) || amount <= 0) {
            throw new Error("Amount must be a positive number.");
        }

        if (
            !this.balances.has(senderAccountId) ||
            !this.balances.has(receiverAccountId)
        ) {
            throw new Error("Sender or receiver account does not exist.");
        }

        if (requestId && this.processedRequests.has(requestId)) {
            return {
                ...this.processedRequests.get(requestId),
                duplicate: true
            };
        }

        const lockKeys = [senderAccountId, receiverAccountId].sort();
        const previousLocks = lockKeys.map(
            key => this.accountLocks.get(key) || Promise.resolve()
        );

        let release;
        const currentLock = new Promise(resolve => {
            release = resolve;
        });

        for (const key of lockKeys) {
            this.accountLocks.set(
                key,
                Promise.all(previousLocks).then(() => currentLock)
            );
        }

        await Promise.all(previousLocks);

        try {
            // Check the request ID again after waiting for other payments.
            if (requestId && this.processedRequests.has(requestId)) {
                return {
                    ...this.processedRequests.get(requestId),
                    duplicate: true
                };
            }

            const senderBalance = this.balances.get(senderAccountId);
            const receiverBalance = this.balances.get(receiverAccountId);

            if (senderBalance < amount) {
                throw new Error("Insufficient balance.");
            }

            // Simulate a failure before committing any balance changes.
            if (simulateFailure) {
                throw new Error("Simulated payment failure.");
            }

            const transaction = {
                transactionId: `TXN${String(this.nextTransactionId).padStart(4, "0")}`,
                senderAccountId,
                receiverAccountId,
                amount,
                status: "SUCCESS",
                requestId: requestId || null,
                timestamp: new Date().toISOString()
            };

            // Commit both balance changes together in this local simulation.
            this.balances.set(senderAccountId, senderBalance - amount);
            this.balances.set(receiverAccountId, receiverBalance + amount);

            this.transactions.push(transaction);
            this.nextTransactionId++;

            if (requestId) {
                this.processedRequests.set(requestId, transaction);
            }

            return { ...transaction, duplicate: false };
        } finally {
            for (const key of lockKeys) {
                if (this.accountLocks.get(key)) {
                    this.accountLocks.delete(key);
                }
            }
            release();
        }
    }
}

if (typeof module !== "undefined" && module.exports) {
    module.exports = PaymentEngine;
}

if (typeof window !== "undefined") {
    window.PaymentEngine = PaymentEngine;
}
