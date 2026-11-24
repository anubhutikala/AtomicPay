package com.atomicpay.backend.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atomicpay.backend.model.Account;
import com.atomicpay.backend.model.PaymentTransaction;
import com.atomicpay.backend.recovery.TransactionState;
import com.atomicpay.backend.repository.AccountRepository;
import com.atomicpay.backend.repository.PaymentTransactionRepository;

@Service
public class PaymentService {

    private final PaymentValidator validator;
    private final AccountRepository accountRepository;
    private final PaymentTransactionRepository transactionRepository;

    public PaymentService(
            AccountRepository accountRepository,
            PaymentTransactionRepository transactionRepository) {

        this.validator = new PaymentValidator();
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public PaymentResult processPayment(
            String orderId,
            String senderId,
            String receiverId,
            double amount,
            String workerName) {

        if (orderId == null || orderId.trim().isEmpty()) {
            return new PaymentResult(false, "Order ID is required");
        }

        String error = validator.validate(senderId, receiverId, amount);

        if (error != null || !Double.isFinite(amount)) {
            return new PaymentResult(false,
                    error != null ? error : "Invalid payment amount");
        }

        BigDecimal paymentAmount = BigDecimal.valueOf(amount);

        // Lock the transaction to prevent duplicate processing
        PaymentTransaction transaction =
                transactionRepository.findByIdForUpdate(orderId).orElse(null);

        if (transaction == null) {
            return new PaymentResult(false, "Payment request not found");
        }

        boolean samePayment =
                transaction.getSenderId().equals(senderId)
                && transaction.getReceiverId().equals(receiverId)
                && transaction.getAmount().compareTo(paymentAmount) == 0;

        if (!samePayment) {
            return new PaymentResult(false,
                    "Order ID already used for a different payment");
        }

        if (transaction.getStatus() == TransactionState.SUCCESS) {
            return new PaymentResult(true,
                    "Payment already processed", orderId);
        }

        if (transaction.getStatus() != TransactionState.PENDING) {
            return new PaymentResult(false,
                    "Payment is not in PENDING state");
        }

        transaction.setStatus(TransactionState.PROCESSING);
        transaction.setWorkerId(workerName);
        transactionRepository.saveAndFlush(transaction);

        Account sender;
        Account receiver;

        // Lock accounts in consistent order
        if (senderId.compareTo(receiverId) < 0) {
            sender = accountRepository.findByAccountIdForUpdate(senderId)
                    .orElse(null);
            receiver = accountRepository.findByAccountIdForUpdate(receiverId)
                    .orElse(null);
        } else {
            receiver = accountRepository.findByAccountIdForUpdate(receiverId)
                    .orElse(null);
            sender = accountRepository.findByAccountIdForUpdate(senderId)
                    .orElse(null);
        }

        if (sender == null) {
            return markFailed(transaction, "Sender account not found");
        }

        if (receiver == null) {
            return markFailed(transaction, "Receiver account not found");
        }

        if (sender.getBalance().compareTo(paymentAmount) < 0) {
            return markFailed(transaction, "Insufficient balance");
        }

        // Transfer money
        sender.setBalance(sender.getBalance().subtract(paymentAmount));
        receiver.setBalance(receiver.getBalance().add(paymentAmount));

        accountRepository.save(sender);
        accountRepository.save(receiver);

        transaction.setStatus(TransactionState.SUCCESS);
        transaction.setFailureReason(null);

        transactionRepository.save(transaction);

        return new PaymentResult(true,
                "Payment completed successfully", orderId);
    }

    private PaymentResult markFailed(
            PaymentTransaction transaction, String reason) {

        transaction.setStatus(TransactionState.FAILED);
        transaction.setFailureReason(reason);

        transactionRepository.save(transaction);

        return new PaymentResult(false, reason);
    }
}