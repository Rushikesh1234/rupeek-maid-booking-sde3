package com.rupeek.maidbooking.domain.model;

import java.math.BigDecimal;

/**
 * Value Object returned after evaluating and executing a booking cancellation refund.
 */
public record RefundResult(
        BigDecimal refundAmount,
        int refundPercentage,
        String policyExplanation,
        String gatewayRefundReference
) {}
