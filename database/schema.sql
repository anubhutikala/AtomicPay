CREATE TABLE accounts (
    account_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    balance NUMERIC(12,2) NOT NULL
);

CREATE TABLE payment_transaction (
    order_id VARCHAR(50) PRIMARY KEY,
    sender_id VARCHAR(50) NOT NULL,
    receiver_id VARCHAR(50) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    worker_id VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (sender_id) REFERENCES accounts(account_id),
    FOREIGN KEY (receiver_id) REFERENCES accounts(account_id)
);

CREATE TABLE logs (
    log_id SERIAL PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    message VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (order_id) REFERENCES payment_transaction(order_id)
);
