
package com.atomicpay.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atomicpay.backend.model.ScheduledPayment;
import com.atomicpay.backend.model.PaymentTransaction;
import com.atomicpay.backend.model.PaymentMessage;
import com.atomicpay.backend.repository.ScheduledPaymentRepository;
import com.atomicpay.backend.repository.PaymentTransactionRepository;
import com.atomicpay.backend.recovery.TransactionState;

@Service
public class ScheduledPaymentService {

    private final ScheduledPaymentRepository scheduledRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentProducer paymentProducer;

    public ScheduledPaymentService(
            ScheduledPaymentRepository scheduledRepository,
            PaymentTransactionRepository transactionRepository,
            PaymentProducer paymentProducer) {

        this.scheduledRepository = scheduledRepository;
        this.transactionRepository = transactionRepository;
        this.paymentProducer = paymentProducer;
    }

    public ScheduledPayment schedulePayment(
            String orderId,
            String senderId,
            String receiverId,
            double amount,
            LocalDateTime scheduledAt) {

        if (orderId == null || orderId.isBlank()
                || senderId == null || senderId.isBlank()
                || receiverId == null || receiverId.isBlank()
                || senderId.equals(receiverId)
                || !Double.isFinite(amount) || amount <= 0
                || scheduledAt == null
                || !scheduledAt.isAfter(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Invalid scheduled payment details");
        }

        if (transactionRepository.existsById(orderId)
                || scheduledRepository.existsByOrderId(orderId)) {

            throw new IllegalArgumentException(
                    "Order ID already exists");
        }

        ScheduledPayment payment = new ScheduledPayment();
        payment.setOrderId(orderId);
        payment.setSenderId(senderId);
        payment.setReceiverId(receiverId);
        payment.setAmount(BigDecimal.valueOf(amount));
        payment.setScheduledAt(scheduledAt);
        payment.setStatus("SCHEDULED");

        return scheduledRepository.save(payment);
    }

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void executeScheduledPayments() {

        List<ScheduledPayment> duePayments =
                scheduledRepository
                    .findByStatusAndScheduledAtLessThanEqual(
                        "SCHEDULED", LocalDateTime.now());

        for (ScheduledPayment payment : duePayments) {

            PaymentTransaction transaction = null;

            try {
                if (transactionRepository.existsById(
                        payment.getOrderId())) {

                    payment.setStatus("FAILED");
                    continue;
                }

                transaction = new PaymentTransaction();

                transaction.setOrderId(payment.getOrderId());
                transaction.setSenderId(payment.getSenderId());
                transaction.setReceiverId(payment.getReceiverId());
                transaction.setAmount(payment.getAmount());
                transaction.setPaymentMethod("SCHEDULED");
                transaction.setStatus(TransactionState.PENDING);
                transaction.setRetryCount(0);

                transactionRepository.saveAndFlush(transaction);

                payment.setStatus("PROCESSING");

                paymentProducer.sendPayment(
                    new PaymentMessage(
                        payment.getOrderId(),
                        payment.getSenderId(),
                        payment.getReceiverId(),
                        payment.getAmount().doubleValue()
                    )
                );

                payment.setStatus("SUBMITTED");

            } catch (Exception e) {

                payment.setStatus("FAILED");

                if (transaction != null) {
                    transaction.setStatus(TransactionState.FAILED);
                    transactionRepository.save(transaction);
                }
            }
        }

        scheduledRepository.saveAll(duePayments);
    }
}
