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

-- =====================================================================
-- PHASE 3 ADDITIONS  (need phase3_additions.sql to be run first)
-- =====================================================================

-- ---- RECOVERY  (Kartikeya) --------------------------------------------
-- NOTE on query 9 above: it returns EVERY PENDING/PROCESSING row, including
-- payments a worker is processing at this very moment. If recovery used it,
-- it would fight the live workers. Use 16 instead: only rows older than N minutes.

-- 16. find stuck payments (older than :minutes). SKIP LOCKED = several recovery
--     threads can run without picking the same row.
SELECT order_id, sender_id, receiver_id, amount, status, worker_id, retry_count
FROM payment_transaction
WHERE status IN ('PENDING', 'PROCESSING')
  AND updated_at < CURRENT_TIMESTAMP - make_interval(mins => :minutes)
ORDER BY updated_at
FOR UPDATE SKIP LOCKED;

-- 17. was money actually moved? (a SUCCESS log is written in the same DB
--     transaction as the debit/credit, so it exists only if the money moved)
SELECT COUNT(*) > 0 AS money_moved FROM logs WHERE order_id = :orderId AND event_type = 'SUCCESS';

-- 18. mark a stuck payment INCOMPLETE with the reason
UPDATE payment_transaction
SET status = 'INCOMPLETE', failure_reason = :reason
WHERE order_id = :orderId AND status IN ('PENDING', 'PROCESSING');

-- ---- RETRY  (Kartikeya) ------------------------------------------------
-- 19. payments eligible for retry. Insufficient-funds failures are skipped on
--     purpose: retrying them can never succeed.
SELECT order_id, sender_id, receiver_id, amount, payment_method, retry_count
FROM payment_transaction
WHERE retry_count < :maxRetries
  AND (status = 'INCOMPLETE'
       OR (status = 'FAILED' AND COALESCE(failure_reason, '') NOT ILIKE 'Insufficient%'))
ORDER BY updated_at
FOR UPDATE SKIP LOCKED;

-- 20. claim one retry (0 rows affected = someone else got it or limit reached)
UPDATE payment_transaction
SET status = 'PENDING', retry_count = retry_count + 1
WHERE order_id = :orderId AND status IN ('FAILED', 'INCOMPLETE') AND retry_count < :maxRetries;

-- 21. record why a payment failed
UPDATE payment_transaction SET status = 'FAILED', failure_reason = :reason WHERE order_id = :orderId;

-- ---- SCHEDULED PAYMENTS  (Aayushi) -------------------------------------
-- 22. create a schedule
INSERT INTO scheduled_payment (sender_id, receiver_id, amount, payment_method, scheduled_at)
VALUES (:senderId, :receiverId, :amount, :method, :scheduledAt);

-- 23. due schedules (scheduler polls this)
SELECT schedule_id, sender_id, receiver_id, amount, payment_method
FROM scheduled_payment
WHERE status = 'WAITING' AND scheduled_at <= CURRENT_TIMESTAMP
ORDER BY scheduled_at
FOR UPDATE SKIP LOCKED;

-- 24. mark executed (after creating the payment_transaction row)
UPDATE scheduled_payment SET status = 'EXECUTED', order_id = :orderId WHERE schedule_id = :scheduleId AND status = 'WAITING';

-- 25. cancel
UPDATE scheduled_payment SET status = 'CANCELLED' WHERE schedule_id = :scheduleId AND status = 'WAITING';

-- ---- TRANSACTION FILTERS + CSV EXPORT  (Shriya) ------------------------
-- 26. every filter is optional: pass NULL to skip it. For CSV export, run the
--     same query without LIMIT/OFFSET.
SELECT order_id, sender_id, receiver_id, amount, payment_method, status, worker_id, failure_reason, created_at
FROM payment_transaction
WHERE (CAST(:status AS VARCHAR)    IS NULL OR status = :status)
  AND (CAST(:accountId AS VARCHAR) IS NULL OR sender_id = :accountId OR receiver_id = :accountId)
  AND (CAST(:method AS VARCHAR)    IS NULL OR payment_method = :method)
  AND (CAST(:minAmount AS NUMERIC) IS NULL OR amount >= :minAmount)
  AND (CAST(:maxAmount AS NUMERIC) IS NULL OR amount <= :maxAmount)
  AND (CAST(:fromDate AS TIMESTAMP) IS NULL OR created_at >= :fromDate)
  AND (CAST(:toDate   AS TIMESTAMP) IS NULL OR created_at <  :toDate)
ORDER BY created_at DESC
LIMIT :limit OFFSET :offset;

-- ---- ANALYTICS / WORKER STATS / HEALTH  (Aayushi, Shriya) -------------
-- 27. summary: count and money per status
SELECT status, COUNT(*) AS total, COALESCE(SUM(amount), 0) AS total_amount
FROM payment_transaction GROUP BY status ORDER BY total DESC;

-- 28. success rate (%)
SELECT ROUND(100.0 * COUNT(*) FILTER (WHERE status = 'SUCCESS') / NULLIF(COUNT(*), 0), 2) AS success_rate_pct
FROM payment_transaction;

-- 29. per day, last 7 days
SELECT CAST(created_at AS DATE) AS day, COUNT(*) AS total,
       COUNT(*) FILTER (WHERE status = 'SUCCESS') AS successful,
       COALESCE(SUM(amount) FILTER (WHERE status = 'SUCCESS'), 0) AS volume
FROM payment_transaction
WHERE created_at >= CURRENT_DATE - INTERVAL '6 days'
GROUP BY CAST(created_at AS DATE) ORDER BY day;

-- 30. per payment method
SELECT payment_method, COUNT(*) AS total, COALESCE(SUM(amount) FILTER (WHERE status = 'SUCCESS'), 0) AS volume
FROM payment_transaction GROUP BY payment_method ORDER BY total DESC;

-- 31. worker statistics
SELECT worker_id, COUNT(*) AS handled,
       COUNT(*) FILTER (WHERE status = 'SUCCESS')    AS successful,
       COUNT(*) FILTER (WHERE status = 'FAILED')     AS failed,
       COUNT(*) FILTER (WHERE status = 'PROCESSING') AS in_progress
FROM payment_transaction WHERE worker_id IS NOT NULL
GROUP BY worker_id ORDER BY worker_id;

-- 32. recent logs, optional event filter
SELECT log_id, order_id, event_type, message, created_at FROM logs
WHERE (CAST(:eventType AS VARCHAR) IS NULL OR event_type = :eventType)
ORDER BY log_id DESC LIMIT :limit;

-- 33. system health (DB reachable + current backlog)
SELECT 1 AS db_up;
SELECT COUNT(*) FILTER (WHERE status = 'PENDING')    AS pending,
       COUNT(*) FILTER (WHERE status = 'PROCESSING') AS processing,
       COUNT(*) FILTER (WHERE status = 'INCOMPLETE') AS incomplete
FROM payment_transaction;

-- ---- INTEGRITY CHECKS FOR TESTS (each must return 0 rows) --------------
-- 34. negative balances
SELECT account_id, balance FROM accounts WHERE balance < 0;
-- 35. SUCCESS payment with no SUCCESS log (money moved but nothing recorded)
SELECT t.order_id FROM payment_transaction t
WHERE t.status = 'SUCCESS' AND NOT EXISTS (SELECT 1 FROM logs l WHERE l.order_id = t.order_id AND l.event_type = 'SUCCESS');
-- 36. non-SUCCESS payment that has a SUCCESS log (inconsistent state)
SELECT t.order_id FROM payment_transaction t
WHERE t.status <> 'SUCCESS' AND EXISTS (SELECT 1 FROM logs l WHERE l.order_id = t.order_id AND l.event_type = 'SUCCESS');
