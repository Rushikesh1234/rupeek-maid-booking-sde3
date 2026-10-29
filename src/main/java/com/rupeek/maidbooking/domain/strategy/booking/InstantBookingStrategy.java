package com.rupeek.maidbooking.domain.strategy.booking;

import com.rupeek.maidbooking.domain.model.BookingType;
import com.rupeek.maidbooking.domain.model.TimeSlot;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resolves an INSTANT booking starting immediately (or Optional scheduledSlot if explicitly supplied
 * as 'right now' timestamp by the client) for the requested duration.
 */
@Component
public class InstantBookingStrategy implements BookingStrategy {

    @Override
    public BookingType getSupportedType() {
        return BookingType.INSTANT;
    }

    @Override
    public List<TimeSlot> resolveTimeSlots(BookingCreationCommand command, Clock clock) {
        if (command.scheduledSlot() != null) {
            return List.of(command.scheduledSlot());
        }
        int duration = command.instantDurationMinutes() != null ? command.instantDurationMinutes() : 60;
        LocalDateTime start = LocalDateTime.now(clock).withSecond(0).withNano(0);
        LocalDateTime end = start.plusMinutes(duration);
        return List.of(new TimeSlot(start, end));
    }
}
