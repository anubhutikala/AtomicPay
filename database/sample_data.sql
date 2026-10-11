-- =====================================================================
-- AtomicPay :: sample_data.sql  (Phase 2)
-- Owner: Kartikeya (Database)
-- Run AFTER schema.sql.
--
-- * Password for EVERY demo user is:  password123   (stored as BCrypt)
-- * Account IDs A001-A005 are the ones ConcurrencyTest.java already uses.
-- * Balances below already include the 3 SUCCESS sample payments, so they
--   are the known "starting point" for tests:
--       A001 8800 | A002 5500 | A003 3700 | A004 6700 | A005 10300 | A006 11000
--   Total money in the system = 46000.00
-- =====================================================================

INSERT INTO users (user_id, username, password_hash, role) VALUES
('U000', 'admin',    '$2a$10$4p70TVKY/igFGFbybF6.4eQTS0.Tp3zkWL7kJxhgklh94BqnD43ga', 'ADMIN'),
('U001', 'anubhuti', '$2a$10$4p70TVKY/igFGFbybF6.4eQTS0.Tp3zkWL7kJxhgklh94BqnD43ga', 'USER'),
('U002', 'aayushi',  '$2a$10$4p70TVKY/igFGFbybF6.4eQTS0.Tp3zkWL7kJxhgklh94BqnD43ga', 'USER'),
('U003', 'shriya',   '$2a$10$4p70TVKY/igFGFbybF6.4eQTS0.Tp3zkWL7kJxhgklh94BqnD43ga', 'USER'),
('U004', 'saloni',   '$2a$10$4p70TVKY/igFGFbybF6.4eQTS0.Tp3zkWL7kJxhgklh94BqnD43ga', 'USER'),
('U005', 'yashika',  '$2a$10$4p70TVKY/igFGFbybF6.4eQTS0.Tp3zkWL7kJxhgklh94BqnD43ga', 'USER'),
('U006', 'karan',    '$2a$10$4p70TVKY/igFGFbybF6.4eQTS0.Tp3zkWL7kJxhgklh94BqnD43ga', 'USER');

INSERT INTO accounts (account_id, user_id, name, balance) VALUES
('A001', 'U001', 'Anubhuti',  8800.00),
('A002', 'U002', 'Aayushi',   5500.00),
('A003', 'U003', 'Shriya',    3700.00),
('A004', 'U004', 'Saloni',    6700.00),
('A005', 'U005', 'Yashika',  10300.00),
('A006', 'U006', 'Karan',    11000.00);

-- A few past transactions in different statuses (for the status display / tests)
INSERT INTO payment_transaction (order_id, sender_id, receiver_id, amount, payment_method, status, worker_id) VALUES
('ORD001', 'A001', 'A002',  500.00, 'UPI',         'SUCCESS',    'worker-1'),
('ORD002', 'A001', 'A003',  700.00, 'CARD',        'SUCCESS',    'worker-2'),
('ORD003', 'A004', 'A005',  300.00, 'NET_BANKING', 'SUCCESS',    'worker-3'),
('ORD004', 'A003', 'A006', 9999.00, 'UPI',         'FAILED',     'worker-1'),   -- insufficient funds
('ORD005', 'A005', 'A001',  250.00, 'UPI',         'PROCESSING', 'worker-3');   -- stuck: no money moved (recovery test case)

INSERT INTO logs (order_id, event_type, message) VALUES
('ORD001', 'CREATED', 'Payment request created'),
('ORD001', 'SUCCESS', 'Transaction completed successfully'),
('ORD002', 'CREATED', 'Payment request created'),
('ORD002', 'SUCCESS', 'Transaction completed successfully'),
('ORD003', 'CREATED', 'Payment request created'),
('ORD003', 'SUCCESS', 'Transaction completed successfully'),
('ORD004', 'CREATED', 'Payment request created'),
('ORD004', 'FAILED',  'Insufficient funds'),
('ORD005', 'CREATED', 'Payment request created'),
('ORD005', 'PROCESSING', 'Worker started processing');
