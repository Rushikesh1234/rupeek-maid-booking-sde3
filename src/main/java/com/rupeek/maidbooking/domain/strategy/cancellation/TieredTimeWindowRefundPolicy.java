package com.rupeek.maidbooking.domain.strategy.cancellation;

import com.rupeek.maidbooking.domain.model.BookingOccurrence;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Time-window based cancellation & refund policy:
 * - Cancelled >= 24 hours before occurrence start: 100% full refund.
 * - Cancelled between 4 and 24 hours before occurrence start: 50% partial refund.
 * - Cancelled < 4 hours before occurrence start: 0% refund (late cancellation fee).
 */
@Component
public class TieredTimeWindowRefundPolicy implements CancellationRefundPolicy {

    @Override
    public RefundEvaluation evaluate(List<BookingOccurrence> cancelledOccurrences, LocalDateTime cancelledAt) {
        if (cancelledOccurrences == null || cancelledOccurrences.isEmpty()) {
            return new RefundEvaluation(BigDecimal.ZERO, 0, "No active occurrences cancelled");
        }

        BigDecimal totalOriginal = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;

        for (BookingOccurrence occ : cancelledOccurrences) {
            totalOriginal = totalOriginal.add(occ.getAmount());
            long hoursBeforeStart = Duration.between(cancelledAt, occ.getTimeSlot().getStartTime()).toHours();

            if (hoursBeforeStart >= 24) {
                totalRefund = totalRefund.add(occ.getAmount());
            } else if (hoursBeforeStart >= 4) {
                totalRefund = totalRefund.add(
                        occ.getAmount().multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP));
            }
        }

        int effectivePct = totalOriginal.compareTo(BigDecimal.ZERO) == 0
                ? 0
                : totalRefund.multiply(BigDecimal.valueOf(100))
                        .divide(totalOriginal, 0, RoundingMode.HALF_UP)
                        .intValue();

        String explanation = String.format(
                "Evaluated %d occurrence(s) under TieredTimeWindowRefundPolicy (>=24h: 100%%, 4-24h: 50%%, <4h: 0%%)",
                cancelledOccurrences.size());

        return new RefundEvaluation(totalRefund.setScale(2, RoundingMode.HALF_UP), effectivePct, explanation);
    }
}
