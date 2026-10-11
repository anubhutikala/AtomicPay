package com.atomicpay.backend.recovery;

import org.springframework.stereotype.Service;

@Service
public class RecoveryService {

    public void markProcessing(RecoveryRecord record) {

        record.setState(TransactionState.PROCESSING);
        record.setMessage("Transaction processing started");
    }

    public void markSuccess(RecoveryRecord record) {

        record.setState(TransactionState.SUCCESS);
        record.setMessage("Transaction completed successfully");
    }

    public void markFailed(RecoveryRecord record, String reason) {

        record.setState(TransactionState.FAILED);
        record.setMessage(reason);
    }

    public void markIncomplete(RecoveryRecord record, String reason) {

        record.setState(TransactionState.INCOMPLETE);
        record.setMessage(reason);
    }

    public void recover(RecoveryRecord record) {

        if (record.getState() == TransactionState.INCOMPLETE) {

            record.setState(TransactionState.FAILED);
            record.setMessage(
                "Recovery checked the incomplete transaction; marked as failed"
            );

        } else {

            record.setMessage(
                "Recovery not required for the current transaction state"
            );
        }
    }
}