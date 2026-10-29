package com.rupeek.maidbooking.application.service;

import com.rupeek.maidbooking.domain.exception.ResourceNotFoundException;
import com.rupeek.maidbooking.domain.exception.SlotConflictException;
import com.rupeek.maidbooking.domain.model.Booking;
import com.rupeek.maidbooking.domain.model.BookingOccurrence;
import com.rupeek.maidbooking.domain.model.BookingStatus;
import com.rupeek.maidbooking.domain.model.BookingType;
import com.rupeek.maidbooking.domain.model.Maid;
import com.rupeek.maidbooking.domain.model.OccurrenceStatus;
import com.rupeek.maidbooking.domain.model.TimeSlot;
import com.rupeek.maidbooking.domain.port.BookingRepository;
import com.rupeek.maidbooking.domain.port.MaidRepository;
import com.rupeek.maidbooking.domain.port.MaidSlotLockManager;
import com.rupeek.maidbooking.domain.strategy.booking.BookingCreationCommand;
import com.rupeek.maidbooking.domain.strategy.booking.BookingStrategy;
import com.rupeek.maidbooking.domain.strategy.pricing.PricingStrategy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Coordinates booking creation across Instant, Scheduled, and Recurring strategies
 * with fine-grained per-maid concurrency locking and dynamic pricing.
 */
@Service
public class BookingOrchestrationService {

    private final MaidRepository maidRepository;
    private final BookingRepository bookingRepository;
    private final MaidSlotLockManager lockManager;
    private final PricingStrategy pricingStrategy;
    private final Map<BookingType, BookingStrategy> strategiesByType;
    private final Clock clock;

    public BookingOrchestrationService(
            MaidRepository maidRepository,
            BookingRepository bookingRepository,
            MaidSlotLockManager lockManager,
            PricingStrategy pricingStrategy,
            List<BookingStrategy> bookingStrategies,
            Clock clock) {
        this.maidRepository = maidRepository;
        this.bookingRepository = bookingRepository;
        this.lockManager = lockManager;
        this.pricingStrategy = pricingStrategy;
        this.clock = clock;
        this.strategiesByType = new EnumMap<>(BookingType.class);
        for (BookingStrategy strategy : bookingStrategies) {
            this.strategiesByType.put(strategy.getSupportedType(), strategy);
        }
    }

    public Booking createBooking(BookingCreationCommand command) {
        Maid maid = maidRepository.findById(command.maidId())
                .orElseThrow(() -> new ResourceNotFoundException("Maid not found with id: " + command.maidId()));

        if (!maid.isActive()) {
            throw new IllegalStateException("Maid " + maid.getId() + " is not currently active");
        }
        if (!maid.hasSkill(command.serviceSkill())) {
            throw new IllegalArgumentException(
                    "Maid " + maid.getName() + " does not offer requested skill: " + command.serviceSkill());
        }

        BookingStrategy strategy = strategiesByType.get(command.bookingType());
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported booking type: " + command.bookingType());
        }

        List<TimeSlot> requestedSlots = strategy.resolveTimeSlots(command, clock);
        for (TimeSlot slot : requestedSlots) {
            if (!maid.isWithinWorkingHours(slot)) {
                throw new SlotConflictException(
                        "Requested slot " + slot + " is outside working hours of maid " + maid.getName());
            }
        }

        // Critical Section: Acquire per-maid lock to prevent simultaneous double-booking
        return lockManager.executeWithMaidLock(maid.getId(), () -> {
            List<BookingOccurrence> conflicts = bookingRepository.findConflictingOccurrences(
                    maid.getId(), requestedSlots);
            if (!conflicts.isEmpty()) {
                BookingOccurrence firstConflict = conflicts.get(0);
                throw new SlotConflictException(
                        "Maid " + maid.getName() + " is already booked for overlapping slot "
                                + firstConflict.getTimeSlot());
            }

            UUID bookingId = UUID.randomUUID();
            List<BookingOccurrence> occurrences = new ArrayList<>(requestedSlots.size());
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (TimeSlot slot : requestedSlots) {
                BigDecimal slotPrice = pricingStrategy.calculateOccurrencePrice(
                        maid, slot, command.bookingType());
                totalAmount = totalAmount.add(slotPrice);
                occurrences.add(new BookingOccurrence(
                        UUID.randomUUID(),
                        bookingId,
                        maid.getId(),
                        slot,
                        slotPrice,
                        OccurrenceStatus.PENDING_PAYMENT));
            }

            Booking booking = new Booking(
                    bookingId,
                    command.customerId(),
                    maid.getId(),
                    command.serviceSkill(),
                    command.bookingType(),
                    occurrences,
                    totalAmount,
                    command.recurrencePattern(),
                    BookingStatus.PENDING_PAYMENT,
                    LocalDateTime.now(clock));

            return bookingRepository.save(booking);
        });
    }

    public Booking getBookingById(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
    }
}
