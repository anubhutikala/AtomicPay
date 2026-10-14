
package com.atomicpay.backend.service;

import com.atomicpay.backend.model.PaymentTransaction;
import com.atomicpay.backend.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final PaymentValidator validator;
    private final PaymentTransactionRepository paymentRepository;

    public PaymentService(PaymentTransactionRepository paymentRepository) {
        this.validator = new PaymentValidator();
        this.paymentRepository = paymentRepository;
    }

    public PaymentResult processPayment(String senderId, String receiverId, double amount) {

        String error = validator.validate(senderId, receiverId, amount);

        if (error != null) {
            return new PaymentResult(false, error);
        }

        PaymentTransaction transaction =
                new PaymentTransaction(senderId, receiverId, amount);

        paymentRepository.save(transaction);

        return new PaymentResult(true, "Payment transaction saved successfully");
    }
}
