package com.rupeek.maidbooking.domain.model;

import com.rupeek.maidbooking.domain.exception.InvalidStateTransitionException;
import com.rupeek.maidbooking.domain.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate Root for a Maid Booking (Instant, Scheduled, or Recurring).
 * Encapsulates state transitions across the booking header and its occurrences.
 */
public class Booking {
    private final UUID id;
    private final UUID customerId;
    private final UUID maidId;
    private final ServiceSkill serviceSkill;
    private final BookingType bookingType;
    private final List<BookingOccurrence> occurrences;
    private final RecurrencePattern recurrencePattern;
    private final LocalDateTime createdAt;
    private BookingStatus status;
    private BigDecimal totalAmount;

    public Booking(
            UUID id,
            UUID customerId,
            UUID maidId,
            ServiceSkill serviceSkill,
            BookingType bookingType,
            List<BookingOccurrence> occurrences,
            BigDecimal totalAmount,
            RecurrencePattern recurrencePattern,
            BookingStatus status,
            LocalDateTime createdAt) {
        if (occurrences == null || occurrences.isEmpty()) {
            throw new IllegalArgumentException("Booking must contain at least one occurrence");
        }
        this.id = id != null ? id : UUID.randomUUID();
        this.customerId = Objects.requireNonNull(customerId, "customerId is required");
        this.maidId = Objects.requireNonNull(maidId, "maidId is required");
        this.serviceSkill = Objects.requireNonNull(serviceSkill, "serviceSkill is required");
        this.bookingType = Objects.requireNonNull(bookingType, "bookingType is required");
        this.occurrences = new ArrayList<>(occurrences);
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount is required");
        this.recurrencePattern = recurrencePattern;
        this.status = status != null ? status : BookingStatus.PENDING_PAYMENT;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getMaidId() {
        return maidId;
    }

    public ServiceSkill getServiceSkill() {
        return serviceSkill;
    }

    public BookingType getBookingType() {
        return bookingType;
    }

    public List<BookingOccurrence> getOccurrences() {
        return Collections.unmodifiableList(occurrences);
    }

    public RecurrencePattern getRecurrencePattern() {
        return recurrencePattern;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Marks the entire booking and all active occurrences as CONFIRMED.
     */
    public void confirmAllOccurrences() {
        transitionHeaderTo(BookingStatus.CONFIRMED);
        for (BookingOccurrence occurrence : occurrences) {
            if (occurrence.getStatus() == OccurrenceStatus.PENDING_PAYMENT) {
                occurrence.confirmPayment();
            }
        }
    }

    /**
     * Marks a single occurrence as paid/confirmed (used in per-occurrence recurring billing).
     */
    public BookingOccurrence confirmSingleOccurrence(UUID occurrenceId) {
        BookingOccurrence target = findOccurrence(occurrenceId);
        target.confirmPayment();
        boolean allActiveConfirmed = occurrences.stream()
                .filter(o -> o.getStatus() != OccurrenceStatus.CANCELLED)
                .allMatch(o -> o.getStatus() == OccurrenceStatus.CONFIRMED);
        if (allActiveConfirmed && status == BookingStatus.PENDING_PAYMENT) {
            transitionHeaderTo(BookingStatus.CONFIRMED);
        }
        return target;
    }

    /**
     * Cancels the entire booking series and frees all non-cancelled occurrences.
     */
    public List<BookingOccurrence> cancelEntireSeries(LocalDateTime cancelledAt) {
        transitionHeaderTo(BookingStatus.CANCELLED);
        List<BookingOccurrence> newlyCancelled = new ArrayList<>();
        for (BookingOccurrence occurrence : occurrences) {
            if (occurrence.getStatus().occupiesInventory()) {
                occurrence.cancel(cancelledAt);
                newlyCancelled.add(occurrence);
            }
        }
        return newlyCancelled;
    }

    /**
     * Cancels a single occurrence of a booking (especially for RECURRING series).
     * If all occurrences become cancelled, transitions the parent booking to CANCELLED;
     * otherwise transitions to PARTIALLY_CANCELLED.
     */
    public BookingOccurrence cancelSingleOccurrence(UUID occurrenceId, LocalDateTime cancelledAt) {
        BookingOccurrence target = findOccurrence(occurrenceId);
        target.cancel(cancelledAt);

        boolean anyRemainingActive = occurrences.stream()
                .anyMatch(o -> o.getStatus().occupiesInventory());
        if (anyRemainingActive) {
            transitionHeaderTo(BookingStatus.PARTIALLY_CANCELLED);
        } else {
            transitionHeaderTo(BookingStatus.CANCELLED);
        }
        return target;
    }

    public BookingOccurrence findOccurrence(UUID occurrenceId) {
        return occurrences.stream()
                .filter(o -> o.getId().equals(occurrenceId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "BookingOccurrence " + occurrenceId + " not found in booking " + id));
    }

    private void transitionHeaderTo(BookingStatus nextState) {
        if (!this.status.canTransitionTo(nextState)) {
            throw new InvalidStateTransitionException(
                    "Invalid booking state transition for booking " + id + ": " + this.status + " -> " + nextState);
        }
        this.status = nextState;
    }
}
