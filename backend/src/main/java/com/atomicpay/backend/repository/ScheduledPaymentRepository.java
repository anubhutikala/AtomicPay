
package com.atomicpay.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.atomicpay.backend.model.ScheduledPayment;

public interface ScheduledPaymentRepository
        extends JpaRepository<ScheduledPayment, Long> {

    List<ScheduledPayment> findByStatusAndScheduledAtLessThanEqual(
            String status, LocalDateTime time);

    boolean existsByOrderId(String orderId);
}
