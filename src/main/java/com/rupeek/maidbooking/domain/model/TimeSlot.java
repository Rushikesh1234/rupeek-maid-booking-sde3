package com.rupeek.maidbooking.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable Value Object representing a concrete date-time interval [startTime, endTime).
 * Half-open interval semantics: [09:00, 11:00) and [11:00, 13:00) do NOT overlap.
 */
public final class TimeSlot {
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    public TimeSlot(LocalDateTime startTime, LocalDateTime endTime) {
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime (" + endTime + ") must be strictly after startTime (" + startTime + ")");
        }
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes < 30 || minutes > 720) {
            throw new IllegalArgumentException("Booking slot duration must be between 30 minutes and 12 hours");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * Checks half-open interval overlap: [startA, endA) overlaps [startB, endB)
     * iff startA < endB AND startB < endA.
     */
    public boolean overlapsWith(TimeSlot other) {
        Objects.requireNonNull(other, "other TimeSlot must not be null");
        return this.startTime.isBefore(other.endTime) && other.startTime.isBefore(this.endTime);
    }

    public BigDecimal durationInHours() {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeSlot that)) return false;
        return Objects.equals(startTime, that.startTime) && Objects.equals(endTime, that.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime);
    }

    @Override
    public String toString() {
        return "[" + startTime + " -> " + endTime + ")";
    }
}
