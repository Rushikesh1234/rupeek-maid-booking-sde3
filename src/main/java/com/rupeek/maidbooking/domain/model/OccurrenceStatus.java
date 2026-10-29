package com.rupeek.maidbooking.domain.model;

import java.util.EnumSet;

/**
 * State machine for an individual BookingOccurrence slot.
 * Only PENDING_PAYMENT and CONFIRMED occupy the maid's inventory.
 * Once CANCELLED, the slot is immediately freed for discovery and re-booking.
 */
public enum OccurrenceStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    COMPLETED;

    public boolean occupiesInventory() {
        return this == PENDING_PAYMENT || this == CONFIRMED;
    }

    public boolean canTransitionTo(OccurrenceStatus target) {
        return switch (this) {
            case PENDING_PAYMENT -> EnumSet.of(CONFIRMED, CANCELLED).contains(target);
            case CONFIRMED -> EnumSet.of(CANCELLED, COMPLETED).contains(target);
            case CANCELLED, COMPLETED -> false;
        };
    }
}
