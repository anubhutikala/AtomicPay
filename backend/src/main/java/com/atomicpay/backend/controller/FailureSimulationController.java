
package com.atomicpay.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.atomicpay.backend.service.PaymentService;

@RestController
@RequestMapping("/api/payments/simulate")
public class FailureSimulationController {

    private final PaymentService paymentService;

    public FailureSimulationController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/failure")
    public ResponseEntity<?> simulateFailure(
            @RequestBody Map<String, String> request) {

        String senderId = request.get("senderId");
        String receiverId = request.get("receiverId");
        String orderId = request.get("orderId");

        try {
            double amount = Double.parseDouble(request.get("amount"));

            if (amount <= 0 || !Double.isFinite(amount)) {
                return ResponseEntity.badRequest()
                        .body("Invalid simulation amount");
            }

            // Deliberately submit an amount greater than the
            // sender's balance to simulate insufficient funds.
            var result = paymentService.processPayment(
                    orderId,
                    senderId,
                    receiverId,
                    amount,
                    "FAILURE_SIMULATOR"
            );

            return ResponseEntity.ok(Map.of(
                    "simulation", "COMPLETED",
                    "paymentStatus",
                    result.isSuccess() ? "SUCCESS" : "FAILED",
                    "message", result.getMessage()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Failure simulation error: " + e.getMessage());
        }
    }
}
