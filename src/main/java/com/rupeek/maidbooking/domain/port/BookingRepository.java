package com.rupeek.maidbooking.domain.port;

import com.rupeek.maidbooking.domain.model.Booking;
import com.rupeek.maidbooking.domain.model.BookingOccurrence;
import com.rupeek.maidbooking.domain.model.TimeSlot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for Bookings and slot conflict queries.
 */
public interface BookingRepository {
    Booking save(Booking booking);

    Optional<Booking> findById(UUID bookingId);

    List<Booking> findByMaidId(UUID maidId);

    /**
     * Returns all active (PENDING_PAYMENT or CONFIRMED) occurrences for a maid
     * that overlap with any of the candidate slots.
     */
    List<BookingOccurrence> findConflictingOccurrences(UUID maidId, List<TimeSlot> candidateSlots);
}
