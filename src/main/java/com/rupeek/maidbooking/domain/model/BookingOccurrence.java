package com.rupeek.maidbooking.domain.model;

import com.rupeek.maidbooking.domain.exception.InvalidStateTransitionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing a single scheduled slot occurrence inside a Booking.
 * For INSTANT and SCHEDULED bookings, a Booking has 1 BookingOccurrence.
 * For RECURRING bookings, a Booking has N BookingOccurrences (each independently cancellable/payable).
 */
public class BookingOccurrence {
    private final UUID id;
    private final UUID bookingId;
    private final UUID maidId;
    private final TimeSlot timeSlot;
    private final BigDecimal amount;
    private OccurrenceStatus status;
    private LocalDateTime cancelledAt;

    public BookingOccurrence(
            UUID id,
            UUID bookingId,
            UUID maidId,
            TimeSlot timeSlot,
            BigDecimal amount,
            OccurrenceStatus status) {
        this.id = id != null ? id : UUID.randomUUID();
        this.bookingId = Objects.requireNonNull(bookingId, "bookingId is required");
        this.maidId = Objects.requireNonNull(maidId, "maidId is required");
        this.timeSlot = Objects.requireNonNull(timeSlot, "timeSlot is required");
        this.amount = Objects.requireNonNull(amount, "amount is required");
        this.status = Objects.requireNonNull(status, "status is required");
    }

    public UUID getId() {
        return id;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public UUID getMaidId() {
        return maidId;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public OccurrenceStatus getStatus() {
        return status;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public boolean conflictsWith(TimeSlot candidateSlot) {
        return status.occupiesInventory() && this.timeSlot.overlapsWith(candidateSlot);
    }

    public void confirmPayment() {
        if (!status.canTransitionTo(OccurrenceStatus.CONFIRMED)) {
            throw new InvalidStateTransitionException(
                    "Cannot confirm occurrence " + id + " from state " + status);
        }
        this.status = OccurrenceStatus.CONFIRMED;
    }

    public void cancel(LocalDateTime cancellationTimestamp) {
        if (!status.canTransitionTo(OccurrenceStatus.CANCELLED)) {
            throw new InvalidStateTransitionException(
                    "Cannot cancel occurrence " + id + " from state " + status);
        }
        this.status = OccurrenceStatus.CANCELLED;
        this.cancelledAt = cancellationTimestamp;
    }
}
