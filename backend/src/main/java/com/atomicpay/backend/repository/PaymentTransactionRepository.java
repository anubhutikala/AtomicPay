package com.atomicpay.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.atomicpay.backend.model.PaymentTransaction;

public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction, String> {

    Optional<PaymentTransaction> findByOrderId(String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PaymentTransaction t WHERE t.orderId = :orderId")
    Optional<PaymentTransaction> findByIdForUpdate(
            @Param("orderId") String orderId);
}