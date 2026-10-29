package com.rupeek.maidbooking.domain.strategy.booking;

import com.rupeek.maidbooking.domain.model.BookingType;
import com.rupeek.maidbooking.domain.model.TimeSlot;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resolves a single future TimeSlot for a SCHEDULED booking.
 */
@Component
public class ScheduledBookingStrategy implements BookingStrategy {

    @Override
    public BookingType getSupportedType() {
        return BookingType.SCHEDULED;
    }

    @Override
    public List<TimeSlot> resolveTimeSlots(BookingCreationCommand command, Clock clock) {
        if (command.scheduledSlot() == null) {
            throw new IllegalArgumentException("scheduledSlot is required for SCHEDULED bookings");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (command.scheduledSlot().getStartTime().isBefore(now)) {
            throw new IllegalArgumentException("Scheduled booking startTime must be in the future");
        }
        return List.of(command.scheduledSlot());
    }
}
