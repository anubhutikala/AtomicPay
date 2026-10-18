package com.atomicpay.backend.recovery;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atomicpay.backend.model.PaymentTransaction;
import com.atomicpay.backend.repository.PaymentTransactionRepository;

@Service
public class RecoveryService {

    private final PaymentTransactionRepository transactionRepository;

    public RecoveryService(PaymentTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // Mark a transaction as PROCESSING
    public void markProcessing(RecoveryRecord record) {
        record.setState(TransactionState.PROCESSING);
        record.setMessage("Transaction is being processed");
    }

    // Mark a transaction as SUCCESS
    public void markSuccess(RecoveryRecord record) {
        record.setState(TransactionState.SUCCESS);
        record.setMessage("Transaction completed successfully");
    }

    // Mark a transaction as FAILED
    public void markFailed(RecoveryRecord record, String reason) {
        record.setState(TransactionState.FAILED);
        record.setMessage(reason);
    }

    // Mark a transaction as INCOMPLETE
    public void markIncomplete(RecoveryRecord record, String reason) {
        record.setState(TransactionState.INCOMPLETE);
        record.setMessage(reason);
    }

    // Recover an in-memory incomplete transaction
    public void recover(RecoveryRecord record) {
        if (record.getState() == TransactionState.INCOMPLETE) {
            record.setState(TransactionState.FAILED);
            record.setMessage(
                    "Recovery checked the incomplete transaction; marked as failed");
        } else {
            record.setMessage("Recovery not required for this transaction");
        }
    }

    // Find transactions that may need recovery
    @Transactional(readOnly = true)
    public List<PaymentTransaction> findIncompleteTransactions() {
        return transactionRepository.findAll().stream()
                .filter(transaction ->
                        transaction.getStatus() == TransactionState.INCOMPLETE
                        || transaction.getStatus() == TransactionState.PROCESSING)
                .toList();
    }

    // Recover manually inserted or otherwise verified stuck transactions
    @Transactional
    public int recoverStuckTransactions() {
        List<PaymentTransaction> transactions = findIncompleteTransactions();
        int recoveredCount = 0;

        for (PaymentTransaction transaction : transactions) {
            if (transaction.getStatus() == TransactionState.PROCESSING
                    || transaction.getStatus() == TransactionState.INCOMPLETE) {

                transaction.setStatus(TransactionState.FAILED);
                transactionRepository.save(transaction);
                recoveredCount++;
            }
        }

        return recoveredCount;
    }
}