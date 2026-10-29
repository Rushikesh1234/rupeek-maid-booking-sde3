package com.rupeek.maidbooking.domain.strategy.booking;

import com.rupeek.maidbooking.domain.model.BookingType;
import com.rupeek.maidbooking.domain.model.TimeSlot;

import java.time.Clock;
import java.util.List;

/**
 * Strategy interface for resolving concrete TimeSlots from a booking request.
 * Adding a new booking type (e.g., FLEXI_PASS or MULTI_DAY_LIVE_IN) only requires
 * adding a new BookingStrategy implementation without altering BookingOrchestrationService.
 */
public interface BookingStrategy {
    BookingType getSupportedType();

    List<TimeSlot> resolveTimeSlots(BookingCreationCommand command, Clock clock);
}
