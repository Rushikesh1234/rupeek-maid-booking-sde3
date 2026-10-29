package com.rupeek.maidbooking.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Explicit state machine for a Booking lifecycle.
 * Prevents invalid state transitions (e.g., paying for a CANCELLED booking
 * or cancelling an already COMPLETED booking).
 */
public enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    PARTIALLY_CANCELLED,
    CANCELLED,
    COMPLETED;

    public boolean canTransitionTo(BookingStatus target) {
        return switch (this) {
            case PENDING_PAYMENT -> EnumSet.of(CONFIRMED, PARTIALLY_CANCELLED, CANCELLED).contains(target);
            case CONFIRMED -> EnumSet.of(PARTIALLY_CANCELLED, CANCELLED, COMPLETED).contains(target);
            case PARTIALLY_CANCELLED -> EnumSet.of(PARTIALLY_CANCELLED, CANCELLED, COMPLETED).contains(target);
            case CANCELLED, COMPLETED -> false;
        };
    }

    public Set<BookingStatus> allowedNextStates() {
        return switch (this) {
            case PENDING_PAYMENT -> EnumSet.of(CONFIRMED, PARTIALLY_CANCELLED, CANCELLED);
            case CONFIRMED -> EnumSet.of(PARTIALLY_CANCELLED, CANCELLED, COMPLETED);
            case PARTIALLY_CANCELLED -> EnumSet.of(PARTIALLY_CANCELLED, CANCELLED, COMPLETED);
            case CANCELLED, COMPLETED -> EnumSet.noneOf(BookingStatus.class);
        };
    }
}
