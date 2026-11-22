-- =====================================================================
-- AtomicPay :: phase3_additions.sql  (Phase 3)           PostgreSQL
-- Owner: Kartikeya (Database)
--
-- Purely ADDITIVE and RE-RUNNABLE. Existing Java entities keep working:
-- no column is renamed/removed and every new column has a default.
--
-- Load order (fresh DB):  schema.sql -> sample_data.sql -> phase3_additions.sql
-- Existing DB with data:  just run phase3_additions.sql
--
-- Why it exists: the Phase 2 schema has no way to know HOW LONG a payment
-- has been PROCESSING, so recovery cannot tell a stuck payment from one a
-- worker is processing right now. It also has no retry counter or failure
-- reason, and nowhere to store scheduled payments.
-- =====================================================================

-- 1. New columns on payment_transaction -------------------------------
ALTER TABLE payment_transaction ADD COLUMN IF NOT EXISTS updated_at     TIMESTAMP;
ALTER TABLE payment_transaction ADD COLUMN IF NOT EXISTS retry_count    INT NOT NULL DEFAULT 0;
ALTER TABLE payment_transaction ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(255);

UPDATE payment_transaction SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE payment_transaction ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE payment_transaction ALTER COLUMN updated_at SET NOT NULL;

-- copy the existing failure message from logs for already-FAILED rows
UPDATE payment_transaction t
   SET failure_reason = l.message
  FROM logs l
 WHERE l.order_id = t.order_id AND l.event_type = 'FAILED'
   AND t.status = 'FAILED' AND t.failure_reason IS NULL;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_txn_retry_count') THEN
    ALTER TABLE payment_transaction ADD CONSTRAINT chk_txn_retry_count CHECK (retry_count >= 0);
  END IF;
END $$;

-- 2. Trigger: updated_at refreshes on ANY update, so no Java change is needed
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_txn_updated ON payment_transaction;
CREATE TRIGGER trg_txn_updated
    BEFORE UPDATE ON payment_transaction
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 3. Scheduled payments (Aayushi's feature). CONFIRM WITH HER before use:
--    skip this table if her scheduler already stores schedules elsewhere.
CREATE TABLE IF NOT EXISTS scheduled_payment (
    schedule_id    SERIAL        PRIMARY KEY,
    sender_id      VARCHAR(50)   NOT NULL REFERENCES accounts(account_id),
    receiver_id    VARCHAR(50)   NOT NULL REFERENCES accounts(account_id),
    amount         NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    payment_method VARCHAR(30)   NOT NULL,
    scheduled_at   TIMESTAMP     NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'WAITING'
                   CHECK (status IN ('WAITING', 'EXECUTED', 'CANCELLED')),
    order_id       VARCHAR(50)   REFERENCES payment_transaction(order_id),  -- set once executed
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sched_not_self CHECK (sender_id <> receiver_id)
);

-- 4. Indexes (PostgreSQL does NOT index foreign keys automatically) -----
CREATE INDEX IF NOT EXISTS idx_accounts_user      ON accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_txn_sender         ON payment_transaction(sender_id);
CREATE INDEX IF NOT EXISTS idx_txn_receiver       ON payment_transaction(receiver_id);
CREATE INDEX IF NOT EXISTS idx_txn_status_updated ON payment_transaction(status, updated_at);
CREATE INDEX IF NOT EXISTS idx_txn_created        ON payment_transaction(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_txn_worker         ON payment_transaction(worker_id);
CREATE INDEX IF NOT EXISTS idx_logs_order         ON logs(order_id);
CREATE INDEX IF NOT EXISTS idx_sched_due          ON scheduled_payment(scheduled_at) WHERE status = 'WAITING';

-- 5. DEMO ONLY: make ORD005 look "stuck for 10 minutes" so the recovery
--    module has a real case to find. Harmless if ORD005 does not exist.
ALTER TABLE payment_transaction DISABLE TRIGGER trg_txn_updated;
UPDATE payment_transaction SET updated_at = CURRENT_TIMESTAMP - INTERVAL '10 minutes'
 WHERE order_id = 'ORD005' AND status = 'PROCESSING';
ALTER TABLE payment_transaction ENABLE TRIGGER trg_txn_updated;
