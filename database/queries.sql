-- =====================================================================
-- AtomicPay :: queries.sql  (Phase 2)
-- Owner: Kartikeya (Database)
-- Basic queries the team needs, with JPA-style :namedParameters so they
-- can go in @Query(value = "...", nativeQuery = true).
-- =====================================================================

-- ---- LOGIN / USERS  (Aayushi) ----------------------------------------
-- 1. find user for login (compare BCrypt hash in Java)
SELECT user_id, username, password_hash, role FROM users WHERE username = :username;

-- 2. accounts of a user
SELECT account_id, name, balance FROM accounts WHERE user_id = :userId;

-- ---- PAYMENT FLOW  (Anubhuti) ----------------------------------------
-- 3. read an account (add FOR UPDATE to row-lock it inside a DB transaction)
SELECT account_id, name, balance FROM accounts WHERE account_id = :accountId;
SELECT account_id, name, balance FROM accounts WHERE account_id = :accountId FOR UPDATE;

-- 4. debit: only succeeds if enough money. Affects 0 rows if balance too low.
UPDATE accounts SET balance = balance - :amount
WHERE account_id = :accountId AND balance >= :amount;

-- 5. credit
UPDATE accounts SET balance = balance + :amount WHERE account_id = :accountId;

-- 6. create the payment record (a duplicate order_id is rejected by the PRIMARY KEY)
INSERT INTO payment_transaction (order_id, sender_id, receiver_id, amount, payment_method, status, worker_id)
VALUES (:orderId, :senderId, :receiverId, :amount, :method, 'PENDING', :workerId);

-- 7. change status (PENDING / PROCESSING / SUCCESS / FAILED / INCOMPLETE)
UPDATE payment_transaction SET status = :status WHERE order_id = :orderId;

-- 8. write a log entry
INSERT INTO logs (order_id, event_type, message) VALUES (:orderId, :eventType, :message);

-- 9. transactions that did not finish (input for the recovery module)
SELECT order_id, sender_id, receiver_id, amount, status, worker_id
FROM payment_transaction WHERE status IN ('PENDING', 'PROCESSING', 'INCOMPLETE');

-- ---- DISPLAY / MONITORING  (Shriya, Aayushi) --------------------------
-- 10. one transaction (status polling)
SELECT order_id, sender_id, receiver_id, amount, payment_method, status, worker_id, created_at
FROM payment_transaction WHERE order_id = :orderId;

-- 11. transactions of an account, newest first
SELECT order_id, sender_id, receiver_id, amount, status, created_at
FROM payment_transaction
WHERE sender_id = :accountId OR receiver_id = :accountId
ORDER BY created_at DESC;

-- 12. all transactions (admin view)
SELECT order_id, sender_id, receiver_id, amount, status, worker_id, created_at
FROM payment_transaction ORDER BY created_at DESC;

-- 13. logs of one transaction
SELECT log_id, event_type, message, created_at FROM logs WHERE order_id = :orderId ORDER BY log_id;

-- 14. count per status (dashboard)
SELECT status, COUNT(*) AS total FROM payment_transaction GROUP BY status;

-- 15. sanity check for tests: total money must never change
SELECT SUM(balance) AS total_money FROM accounts;
