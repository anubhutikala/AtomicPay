package com.atomicpay.backend.service;

import java.math.BigDecimal;
import java.util.UUID;

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
            String senderId,
            String receiverId,
            double amount,
            String workerName) {

        // Validate the payment request
        String error = validator.validate(senderId, receiverId, amount);

        if (error != null) {
            return new PaymentResult(false, error);
        }

        if (!Double.isFinite(amount)) {
            return new PaymentResult(
                    false, "Payment amount must be finite");
        }

        BigDecimal paymentAmount = BigDecimal.valueOf(amount);

        Account sender;
        Account receiver;

        // Acquire account locks in a consistent order
        // to reduce the possibility of deadlocks.
        if (senderId.compareTo(receiverId) < 0) {

            sender = accountRepository
                    .findByAccountIdForUpdate(senderId)
                    .orElse(null);

            receiver = accountRepository
                    .findByAccountIdForUpdate(receiverId)
                    .orElse(null);

        } else {

            receiver = accountRepository
                    .findByAccountIdForUpdate(receiverId)
                    .orElse(null);

            sender = accountRepository
                    .findByAccountIdForUpdate(senderId)
                    .orElse(null);
        }

        // Check that both accounts exist
        if (sender == null) {
            return new PaymentResult(false, "Sender account not found");
        }

        if (receiver == null) {
            return new PaymentResult(false, "Receiver account not found");
        }

        // Check available balance
        if (sender.getBalance().compareTo(paymentAmount) < 0) {
            return new PaymentResult(false, "Insufficient balance");
        }

        // Create a transaction record
        PaymentTransaction transaction = new PaymentTransaction();

        transaction.setOrderId("ORD-" + UUID.randomUUID());
        transaction.setSenderId(senderId);
        transaction.setReceiverId(receiverId);
        transaction.setAmount(paymentAmount);
        transaction.setPaymentMethod("INTERNAL");
        transaction.setStatus(TransactionState.PROCESSING);
        transaction.setWorkerId(workerName);

        transactionRepository.saveAndFlush(transaction);

        // Update account balances
        sender.setBalance(sender.getBalance().subtract(paymentAmount));
        receiver.setBalance(receiver.getBalance().add(paymentAmount));

        accountRepository.save(sender);
        accountRepository.save(receiver);

        // Mark the transaction as successful
        transaction.setStatus(TransactionState.SUCCESS);
        transactionRepository.save(transaction);

        return new PaymentResult(
                true,
                "Payment completed successfully",
                transaction.getOrderId());
    }
}