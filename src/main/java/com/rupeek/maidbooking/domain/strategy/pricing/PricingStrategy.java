package com.rupeek.maidbooking.domain.strategy.pricing;

import com.rupeek.maidbooking.domain.model.BookingType;
import com.rupeek.maidbooking.domain.model.Maid;
import com.rupeek.maidbooking.domain.model.TimeSlot;

import java.math.BigDecimal;

/**
 * Pluggable Pricing Strategy (Bonus Requirement).
 * Computes per-occurrence price accounting for duration, booking type, and dynamic surge hours.
 */
public interface PricingStrategy {
    BigDecimal calculateOccurrencePrice(Maid maid, TimeSlot slot, BookingType bookingType);
}
