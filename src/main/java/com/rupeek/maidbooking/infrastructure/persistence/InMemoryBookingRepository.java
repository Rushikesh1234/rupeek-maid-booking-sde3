package com.rupeek.maidbooking.infrastructure.persistence;

import com.rupeek.maidbooking.domain.model.Booking;
import com.rupeek.maidbooking.domain.model.BookingOccurrence;
import com.rupeek.maidbooking.domain.model.TimeSlot;
import com.rupeek.maidbooking.domain.port.BookingRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBookingRepository implements BookingRepository {

    private final Map<UUID, Booking> bookings = new ConcurrentHashMap<>();

    @Override
    public Booking save(Booking booking) {
        bookings.put(booking.getId(), booking);
        return booking;
    }

    @Override
    public Optional<Booking> findById(UUID bookingId) {
        return Optional.ofNullable(bookings.get(bookingId));
    }

    @Override
    public List<Booking> findByMaidId(UUID maidId) {
        return bookings.values().stream()
                .filter(b -> b.getMaidId().equals(maidId))
                .toList();
    }

    @Override
    public List<BookingOccurrence> findConflictingOccurrences(UUID maidId, List<TimeSlot> candidateSlots) {
        List<BookingOccurrence> conflicts = new ArrayList<>();
        for (Booking booking : bookings.values()) {
            if (!booking.getMaidId().equals(maidId)) {
                continue;
            }
            for (BookingOccurrence existing : booking.getOccurrences()) {
                if (!existing.getStatus().occupiesInventory()) {
                    continue;
                }
                for (TimeSlot candidate : candidateSlots) {
                    if (existing.conflictsWith(candidate)) {
                        conflicts.add(existing);
                        break;
                    }
                }
            }
        }
        return conflicts;
    }
}
