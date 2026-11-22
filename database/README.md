# AtomicPay – Database (Kartikeya)

| File | Status | Purpose |
|---|---|---|
| `schema.sql` | unchanged (Phase 2) | users, accounts, payment_transaction, logs |
| `sample_data.sql` | unchanged (Phase 2) | demo users A001-A006, 5 sample payments |
| `phase3_additions.sql` | NEW | updated_at + retry_count + failure_reason, trigger, scheduled_payment table, indexes |
| `queries.sql` | queries 1-15 unchanged, 16-36 NEW | recovery, retry, scheduler, filters/CSV, analytics, worker stats, health, integrity checks |

## Load order
    psql -U postgres -d atomicpay -f schema.sql
    psql -U postgres -d atomicpay -f sample_data.sql
    psql -U postgres -d atomicpay -f phase3_additions.sql   # safe to re-run; safe on a DB that already has data

## Notes
- Additions are backward compatible: no column renamed or removed, new columns have defaults, so existing Java entities keep working.
- `updated_at` is refreshed by a DB trigger, so recovery can find payments stuck in PROCESSING without any Java change.
- `scheduled_payment` is optional: confirm with Aayushi before relying on it.
