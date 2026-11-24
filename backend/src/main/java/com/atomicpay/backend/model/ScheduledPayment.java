
package com.atomicpay.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "scheduled_payments")
public class ScheduledPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;
    private String senderId;
    private String receiverId;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    private LocalDateTime scheduledAt;

    private String status = "SCHEDULED";

    public ScheduledPayment() {}

    public Long getId() { return id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
    }
}
