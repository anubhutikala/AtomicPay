package com.atomicpay.backend.recovery;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atomicpay.backend.model.PaymentMessage;
import com.atomicpay.backend.model.PaymentTransaction;
import com.atomicpay.backend.repository.PaymentTransactionRepository;
import com.atomicpay.backend.service.PaymentProducer;

@Service
public class RecoveryService {

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentProducer paymentProducer;

    public RecoveryService(
            PaymentTransactionRepository transactionRepository,
            PaymentProducer paymentProducer) {

        this.transactionRepository = transactionRepository;
        this.paymentProducer = paymentProducer;
    }

    public void markProcessing(RecoveryRecord record) {
        record.setState(TransactionState.PROCESSING);
        record.setMessage("Transaction is being processed");
    }

    public void markSuccess(RecoveryRecord record) {
        record.setState(TransactionState.SUCCESS);
        record.setMessage("Transaction completed successfully");
    }

    public void markFailed(RecoveryRecord record, String reason) {
        record.setState(TransactionState.FAILED);
        record.setMessage(reason);
    }

    public void markIncomplete(RecoveryRecord record, String reason) {
        record.setState(TransactionState.INCOMPLETE);
        record.setMessage(reason);
    }

    public void recover(RecoveryRecord record) {
        if (record.getState() == TransactionState.INCOMPLETE) {
            record.setState(TransactionState.FAILED);
            record.setMessage(
                    "Recovery checked the incomplete transaction; marked as failed");
        } else {
            record.setMessage("Recovery not required for this transaction");
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentTransaction> findIncompleteTransactions() {
        return transactionRepository.findAll().stream()
                .filter(transaction ->
                        transaction.getStatus() == TransactionState.INCOMPLETE
                        || transaction.getStatus() == TransactionState.PROCESSING)
                .toList();
    }

    // Retry all pending transactions through RabbitMQ
    public int retryPendingTransactions() {

        List<PaymentTransaction> pending =
                transactionRepository.findAll().stream()
                        .filter(transaction ->
                                transaction.getStatus() == TransactionState.PENDING)
                        .toList();

        int count = 0;

        for (PaymentTransaction transaction : pending) {

            try {
                PaymentMessage message = new PaymentMessage(
                        transaction.getOrderId(),
                        transaction.getSenderId(),
                        transaction.getReceiverId(),
                        transaction.getAmount().doubleValue()
                );

                paymentProducer.sendPayment(message);
                count++;

            } catch (Exception e) {
                System.err.println(
                        "Could not retry transaction "
                        + transaction.getOrderId() + ": " + e.getMessage());
            }
        }

        return count;
    }

    @Transactional
    public int recoverStuckTransactions() {

        List<PaymentTransaction> transactions =
                findIncompleteTransactions();

        int recoveredCount = 0;

        for (PaymentTransaction transaction : transactions) {

            if (transaction.getStatus() == TransactionState.PROCESSING
                    || transaction.getStatus() == TransactionState.INCOMPLETE) {

                transaction.setStatus(TransactionState.FAILED);
                transaction.setFailureReason(
                        "Transaction requires manual recovery");

                transactionRepository.save(transaction);
                recoveredCount++;
            }
        }

        return recoveredCount;
    }
}