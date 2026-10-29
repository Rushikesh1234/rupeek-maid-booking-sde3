package com.rupeek.maidbooking.domain.strategy.cancellation;

import com.rupeek.maidbooking.domain.model.BookingOccurrence;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Pluggable policy for calculating refunds when a booking or single occurrence is cancelled.
 */
public interface CancellationRefundPolicy {

    record RefundEvaluation(BigDecimal refundableAmount, int refundPercentage, String reason) {}

    RefundEvaluation evaluate(List<BookingOccurrence> cancelledOccurrences, LocalDateTime cancelledAt);
}
