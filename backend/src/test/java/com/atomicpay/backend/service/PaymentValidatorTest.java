
package com.atomicpay.backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentValidatorTest {

    private final PaymentValidator validator = new PaymentValidator();

    @Test
    void validPaymentShouldPass() {
        assertNull(validator.validate("USER1", "USER2", 100));
    }

    @Test
    void negativeAmountShouldFail() {
        assertNotNull(validator.validate("USER1", "USER2", -100));
    }

    @Test
    void zeroAmountShouldFail() {
        assertNotNull(validator.validate("USER1", "USER2", 0));
    }

    @Test
    void blankSenderShouldFail() {
        assertNotNull(validator.validate("", "USER2", 100));
    }

    @Test
    void sameSenderAndReceiverShouldFail() {
        assertNotNull(validator.validate("USER1", "USER1", 100));
    }
}
