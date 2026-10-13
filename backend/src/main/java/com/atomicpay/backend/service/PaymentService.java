package com.atomicpay.backend.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atomicpay.backend.model.Account;
import com.atomicpay.backend.repository.AccountRepository;

@Service
public class PaymentService {

    private final PaymentValidator validator;
    private final AccountRepository accountRepository;

    public PaymentService(AccountRepository accountRepository) {
        this.validator = new PaymentValidator();
        this.accountRepository = accountRepository;
    }

    @Transactional
    public PaymentResult processPayment(
            String senderId, String receiverId, double amount) {

        String error = validator.validate(senderId, receiverId, amount);

        if (error != null) {
            return new PaymentResult(false, error);
        }

        if (!Double.isFinite(amount)) {
            return new PaymentResult(false, "Payment amount must be finite");
        }

        BigDecimal paymentAmount = BigDecimal.valueOf(amount);

        Account sender = accountRepository
                .findByAccountIdForUpdate(senderId)
                .orElse(null);

        if (sender == null) {
            return new PaymentResult(false, "Sender account not found");
        }

        Account receiver = accountRepository
                .findByAccountIdForUpdate(receiverId)
                .orElse(null);

        if (receiver == null) {
            return new PaymentResult(false, "Receiver account not found");
        }

        if (sender.getBalance().compareTo(paymentAmount) < 0) {
            return new PaymentResult(false, "Insufficient balance");
        }

        sender.setBalance(sender.getBalance().subtract(paymentAmount));
        receiver.setBalance(receiver.getBalance().add(paymentAmount));

        accountRepository.save(sender);
        accountRepository.save(receiver);

        return new PaymentResult(true, "Payment completed successfully");
    }
}