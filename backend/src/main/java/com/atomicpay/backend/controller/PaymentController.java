package com.atomicpay.backend.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.atomicpay.backend.dto.PaymentRequest;
import com.atomicpay.backend.model.PaymentMessage;
import com.atomicpay.backend.model.PaymentTransaction;
import com.atomicpay.backend.recovery.RecoveryService;
import com.atomicpay.backend.recovery.TransactionState;
import com.atomicpay.backend.repository.PaymentTransactionRepository;
import com.atomicpay.backend.service.PaymentProducer;
import com.atomicpay.backend.service.WorkerMonitor;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = {
        "http://localhost:5500",
        "http://127.0.0.1:5500"
})
public class PaymentController {

    private final PaymentProducer paymentProducer;
    private final PaymentTransactionRepository transactionRepository;
    private final RecoveryService recoveryService;
    private final WorkerMonitor workerMonitor;

    public PaymentController(
            PaymentProducer paymentProducer,
            PaymentTransactionRepository transactionRepository,
            RecoveryService recoveryService,
            WorkerMonitor workerMonitor) {

        this.paymentProducer = paymentProducer;
        this.transactionRepository = transactionRepository;
        this.recoveryService = recoveryService;
        this.workerMonitor = workerMonitor;
    }

    @GetMapping("/status/{orderId}")
    public ResponseEntity<?> getPaymentStatus(
            @PathVariable String orderId) {

        return transactionRepository.findById(orderId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Payment API is working!");
    }

    @GetMapping("/history")
    public ResponseEntity<List<PaymentTransaction>> getTransactionHistory() {
        return ResponseEntity.ok(transactionRepository.findAll());
    }

    @GetMapping("/workers/stats")
    public ResponseEntity<Map<String, Object>> getWorkerStats() {

        Map<String, Object> stats = new LinkedHashMap<>();

        Map<String, Long> workerTasks =
                workerMonitor.getWorkerTasks()
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().get()
                        ));

        stats.put("activeWorkers", workerMonitor.getActiveWorkers());
        stats.put("totalAttempts", workerMonitor.getTotalAttempts());
        stats.put("successfulAttempts",
                workerMonitor.getSuccessfulAttempts());
        stats.put("failedAttempts", workerMonitor.getFailedAttempts());
        stats.put("workerTasks", workerTasks);

        return ResponseEntity.ok(stats);
    }

    @PostMapping
    public ResponseEntity<?> makePayment(
            @RequestBody PaymentRequest request) {

        if (request == null
                || request.getOrderId() == null
                || request.getOrderId().trim().isEmpty()
                || request.getSenderId() == null
                || request.getReceiverId() == null
                || !Double.isFinite(request.getAmount())
                || request.getAmount() <= 0
                || request.getSenderId().equals(request.getReceiverId())) {

            return ResponseEntity.badRequest()
                    .body("Invalid payment request.");
        }

        try {
            String orderId = request.getOrderId();

            PaymentTransaction existing =
                    transactionRepository.findById(orderId).orElse(null);

            if (existing != null) {

                boolean samePayment =
                        existing.getSenderId().equals(request.getSenderId())
                        && existing.getReceiverId().equals(request.getReceiverId())
                        && existing.getAmount().compareTo(
                                BigDecimal.valueOf(request.getAmount())) == 0;

                if (!samePayment) {
                    return ResponseEntity.badRequest()
                            .body("Order ID already used for another payment.");
                }

                if (existing.getStatus() == TransactionState.SUCCESS) {
                    return ResponseEntity.ok(
                            "Payment already completed: " + orderId);
                }

                if (existing.getStatus() == TransactionState.FAILED) {
                    return ResponseEntity.badRequest()
                            .body("Payment has already failed.");
                }

                if (existing.getStatus() == TransactionState.PROCESSING) {
                    return ResponseEntity.accepted()
                            .body("Payment is being processed: " + orderId);
                }

                // Existing PENDING request: republish it
            } else {

                // Save PENDING transaction before publishing
                PaymentTransaction transaction = new PaymentTransaction();

                transaction.setOrderId(orderId);
                transaction.setSenderId(request.getSenderId());
                transaction.setReceiverId(request.getReceiverId());
                transaction.setAmount(
                        BigDecimal.valueOf(request.getAmount()));
                transaction.setPaymentMethod("INTERNAL");
                transaction.setStatus(TransactionState.PENDING);
                transaction.setRetryCount(0);

                transactionRepository.saveAndFlush(transaction);
            }

            PaymentMessage message = new PaymentMessage(
                    orderId,
                    request.getSenderId(),
                    request.getReceiverId(),
                    request.getAmount()
            );

            paymentProducer.sendPayment(message);

            return ResponseEntity.accepted()
                    .body("Payment request accepted. Order ID: " + orderId);

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body("Unable to submit payment request.");
        }
    }

    @PostMapping("/recovery")
    public ResponseEntity<String> recoverTransactions() {

        int recoveredCount =
                recoveryService.recoverStuckTransactions();

        return ResponseEntity.ok(
                "Recovery completed. Transactions marked as FAILED: "
                        + recoveredCount);
    }
}