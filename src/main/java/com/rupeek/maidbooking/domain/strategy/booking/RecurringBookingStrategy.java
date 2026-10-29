package com.rupeek.maidbooking.domain.strategy.booking;

import com.rupeek.maidbooking.domain.model.BookingType;
import com.rupeek.maidbooking.domain.model.TimeSlot;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

/**
 * Expands a RecurrencePattern into concrete TimeSlots across the recurrence window.
 */
@Component
public class RecurringBookingStrategy implements BookingStrategy {

    @Override
    public BookingType getSupportedType() {
        return BookingType.RECURRING;
    }

    @Override
    public List<TimeSlot> resolveTimeSlots(BookingCreationCommand command, Clock clock) {
        if (command.recurrencePattern() == null) {
            throw new IllegalArgumentException("recurrencePattern is required for RECURRING bookings");
        }
        return command.recurrencePattern().generateOccurrences();
    }
}
