
# AtomicPay - Test Plan

## 1. Purpose

This document records the frontend validation tests and the
backend transaction tests planned for AtomicPay.

## 2. Frontend Validation Tests

| ID | Test case | Expected result |
|---|---|---|
| UI-01 | Submit with an empty sender account | Form prevents invalid submission |
| UI-02 | Submit with an empty receiver account | Form prevents invalid submission |
| UI-03 | Use the same sender and receiver account | Validation message appears |
| UI-04 | Enter an amount of zero or less | Invalid amount is rejected |
| UI-05 | Enter a valid-looking payment request | Frontend sends a request to the configured API |
| UI-06 | Backend is unavailable | Error is shown; no fake successful payment is added |
| UI-07 | Backend returns a transaction response | Returned transaction details appear in history |

## 3. Backend Integration Tests

These tests require the backend API and database to be available.

| ID | Test case | Expected result |
|---|---|---|
| API-01 | Submit a valid payment | Transaction result matches the server response |
| API-02 | Submit a payment with insufficient balance | Transaction is rejected without an incorrect balance change |
| API-03 | Send multiple payments concurrently | No lost updates or inconsistent balances |
| API-04 | Submit the same request twice | Duplicate processing is prevented if idempotency is implemented |
| API-05 | Simulate a failure during a transaction | Partial balance changes are rolled back |
| API-06 | Restart or recover after a simulated failure | Account balances and transaction records remain consistent |
| API-07 | Compare balances before and after a successful transfer | Sender debit equals receiver credit |

## 4. Recording Results

For each test, record:

- Test ID
- Date and environment
- Actual result
- PASS or FAIL
- Screenshot or log reference
- Notes about any defect

Do not mark a test as PASS until it has actually been executed.

## 5. Current Limitations

The API endpoint and request/response fields must be checked against
the backend controller and DTO before integration testing.

Concurrency, duplicate-request protection, rollback and recovery
require backend/database tests. A frontend-only test cannot prove
these properties.

