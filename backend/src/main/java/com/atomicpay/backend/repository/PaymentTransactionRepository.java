package com.atomicpay.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.atomicpay.backend.model.PaymentTransaction;

public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction, String> {
}