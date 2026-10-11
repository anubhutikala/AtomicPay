-- =====================================================================
-- AtomicPay :: schema.sql  (Phase 2)           PostgreSQL
-- Owner: Kartikeya (Database)
-- Re-runnable: drops and recreates the 4 tables. DEV USE ONLY.
-- Load order: schema.sql -> sample_data.sql
-- =====================================================================

DROP TABLE IF EXISTS logs;
DROP TABLE IF EXISTS payment_transaction;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    user_id       VARCHAR(50)  PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,              -- BCrypt hash
    role          VARCHAR(20)  NOT NULL,              -- 'USER' or 'ADMIN'
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE accounts (
    account_id VARCHAR(50)   PRIMARY KEY,
    user_id    VARCHAR(50)   NOT NULL,
    name       VARCHAR(100)  NOT NULL,
    balance    NUMERIC(12,2) NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT chk_accounts_balance CHECK (balance >= 0)   -- balance can never go negative
);

CREATE TABLE payment_transaction (
    order_id       VARCHAR(50)   PRIMARY KEY,         -- unique per payment request
    sender_id      VARCHAR(50)   NOT NULL,
    receiver_id    VARCHAR(50)   NOT NULL,
    amount         NUMERIC(12,2) NOT NULL,
    payment_method VARCHAR(30)   NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    worker_id      VARCHAR(50),
    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (sender_id)   REFERENCES accounts(account_id),
    FOREIGN KEY (receiver_id) REFERENCES accounts(account_id),
    CONSTRAINT chk_txn_amount CHECK (amount > 0),
    CONSTRAINT chk_txn_not_self CHECK (sender_id <> receiver_id),
    -- same values as the Java enum recovery.TransactionState
    CONSTRAINT chk_txn_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'INCOMPLETE')
    )
);

CREATE TABLE logs (
    log_id     SERIAL       PRIMARY KEY,
    order_id   VARCHAR(50)  NOT NULL,
    event_type VARCHAR(50)  NOT NULL,
    message    VARCHAR(255),
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (order_id) REFERENCES payment_transaction(order_id)
);
