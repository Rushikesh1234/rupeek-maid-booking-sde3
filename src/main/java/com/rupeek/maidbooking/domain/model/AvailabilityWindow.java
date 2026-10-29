package com.rupeek.maidbooking.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Value Object modeling a maid's working shift on a given day of the week.
 */
public final class AvailabilityWindow {
    private final DayOfWeek dayOfWeek;
    private final LocalTime startTime;
    private final LocalTime endTime;

    public AvailabilityWindow(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Window endTime must be after startTime");
        }
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    /**
     * Returns true if the requested TimeSlot falls on the same calendar day and
     * lies completely within [startTime, endTime].
     */
    public boolean covers(TimeSlot slot) {
        if (!slot.getStartTime().toLocalDate().equals(slot.getEndTime().toLocalDate())) {
            return false;
        }
        if (slot.getStartTime().getDayOfWeek() != this.dayOfWeek) {
            return false;
        }
        LocalTime reqStart = slot.getStartTime().toLocalTime();
        LocalTime reqEnd = slot.getEndTime().toLocalTime();
        return !reqStart.isBefore(this.startTime) && !reqEnd.isAfter(this.endTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AvailabilityWindow that)) return false;
        return dayOfWeek == that.dayOfWeek
                && Objects.equals(startTime, that.startTime)
                && Objects.equals(endTime, that.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dayOfWeek, startTime, endTime);
    }
}
