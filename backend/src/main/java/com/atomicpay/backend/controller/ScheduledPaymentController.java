
package com.atomicpay.backend.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.atomicpay.backend.model.ScheduledPayment;
import com.atomicpay.backend.service.ScheduledPaymentService;

@RestController
@RequestMapping("/api/payments/scheduled")
public class ScheduledPaymentController {

    private final ScheduledPaymentService scheduledService;

    public ScheduledPaymentController(
            ScheduledPaymentService scheduledService) {
        this.scheduledService = scheduledService;
    }

    @PostMapping
    public ResponseEntity<?> schedulePayment(
            @RequestBody Map<String, String> request) {

        try {
            if (request == null
                    || request.get("amount") == null
                    || request.get("scheduledAt") == null) {

                return ResponseEntity.badRequest()
                        .body("Amount and scheduledAt are required");
            }

            ScheduledPayment payment =
                    scheduledService.schedulePayment(
                        request.get("orderId"),
                        request.get("senderId"),
                        request.get("receiverId"),
                        Double.parseDouble(request.get("amount")),
                        LocalDateTime.parse(
                            request.get("scheduledAt"))
                    );

            return ResponseEntity.accepted().body(payment);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Unable to schedule payment: "
                            + e.getMessage());
        }
    }
}
