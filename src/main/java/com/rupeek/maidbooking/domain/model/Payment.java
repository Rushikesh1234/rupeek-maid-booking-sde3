package com.rupeek.maidbooking.domain.model;

import com.rupeek.maidbooking.domain.exception.InvalidStateTransitionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity modeling a Payment transaction with idempotency protection and refund tracking.
 */
public class Payment {
    private final UUID id;
    private final UUID bookingId;
    private final UUID occurrenceId; // Nullable: null means full-booking payment; non-null means single-occurrence payment
    private final PaymentMethodType paymentMethod;
    private final BigDecimal amount;
    private final String idempotencyKey;
    private final String gatewayReference;
    private final LocalDateTime createdAt;
    private PaymentStatus status;
    private BigDecimal refundedAmount;

    public Payment(
            UUID id,
            UUID bookingId,
            UUID occurrenceId,
            PaymentMethodType paymentMethod,
            BigDecimal amount,
            String idempotencyKey,
            String gatewayReference,
            PaymentStatus status,
            BigDecimal refundedAmount,
            LocalDateTime createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.bookingId = Objects.requireNonNull(bookingId, "bookingId is required");
        this.occurrenceId = occurrenceId;
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "paymentMethod is required");
        this.amount = Objects.requireNonNull(amount, "amount is required");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey is required for payment safety");
        }
        this.idempotencyKey = idempotencyKey.trim();
        this.gatewayReference = Objects.requireNonNull(gatewayReference, "gatewayReference is required");
        this.status = Objects.requireNonNull(status, "status is required");
        this.refundedAmount = refundedAmount != null ? refundedAmount : BigDecimal.ZERO;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public UUID getOccurrenceId() {
        return occurrenceId;
    }

    public PaymentMethodType getPaymentMethod() {
        return paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public BigDecimal getRefundableBalance() {
        return amount.subtract(refundedAmount);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void recordRefund(BigDecimal refundDelta) {
        if (refundDelta.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("refundDelta cannot be negative");
        }
        if (status != PaymentStatus.SUCCESS && status != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new InvalidStateTransitionException("Cannot refund payment in status " + status);
        }
        BigDecimal updated = this.refundedAmount.add(refundDelta);
        if (updated.compareTo(this.amount) > 0) {
            throw new IllegalArgumentException("Total refunded amount cannot exceed original payment amount");
        }
        this.refundedAmount = updated;
        if (this.refundedAmount.compareTo(this.amount) == 0) {
            this.status = PaymentStatus.REFUNDED;
        } else if (this.refundedAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = PaymentStatus.PARTIALLY_REFUNDED;
        }
    }
}
