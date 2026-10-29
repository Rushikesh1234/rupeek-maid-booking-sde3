package com.rupeek.maidbooking.domain.strategy.pricing;

import com.rupeek.maidbooking.domain.model.BookingType;
import com.rupeek.maidbooking.domain.model.Maid;
import com.rupeek.maidbooking.domain.model.TimeSlot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

/**
 * Default pluggable pricing strategy:
 * 1. Base price = Maid's baseHourlyRate * slot duration in hours.
 * 2. Peak-hour surge (07:00-10:00 or 18:00-21:00): 1.20x multiplier.
 * 3. Instant booking express convenience multiplier: 1.15x.
 * 4. Recurring series loyalty discount: 0.90x (10% discount per occurrence).
 */
@Component
public class DynamicTieredPricingStrategy implements PricingStrategy {

    private static final LocalTime MORNING_PEAK_START = LocalTime.of(7, 0);
    private static final LocalTime MORNING_PEAK_END = LocalTime.of(10, 0);
    private static final LocalTime EVENING_PEAK_START = LocalTime.of(18, 0);
    private static final LocalTime EVENING_PEAK_END = LocalTime.of(21, 0);

    private static final BigDecimal PEAK_SURGE_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal INSTANT_MULTIPLIER = new BigDecimal("1.15");
    private static final BigDecimal RECURRING_DISCOUNT_MULTIPLIER = new BigDecimal("0.90");

    @Override
    public BigDecimal calculateOccurrencePrice(Maid maid, TimeSlot slot, BookingType bookingType) {
        BigDecimal price = maid.getBaseHourlyRate().multiply(slot.durationInHours());

        if (isPeakHour(slot.getStartTime().toLocalTime())) {
            price = price.multiply(PEAK_SURGE_MULTIPLIER);
        }

        if (bookingType == BookingType.INSTANT) {
            price = price.multiply(INSTANT_MULTIPLIER);
        } else if (bookingType == BookingType.RECURRING) {
            price = price.multiply(RECURRING_DISCOUNT_MULTIPLIER);
        }

        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isPeakHour(LocalTime startTime) {
        boolean morningPeak = !startTime.isBefore(MORNING_PEAK_START) && startTime.isBefore(MORNING_PEAK_END);
        boolean eveningPeak = !startTime.isBefore(EVENING_PEAK_START) && startTime.isBefore(EVENING_PEAK_END);
        return morningPeak || eveningPeak;
    }
}
