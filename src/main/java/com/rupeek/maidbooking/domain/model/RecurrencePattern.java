package com.rupeek.maidbooking.domain.model;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Value Object modeling a recurring schedule (e.g., every weekday at 09:00 for 2 hours,
 * or every Sunday from 2026-11-01 to 2026-11-30).
 */
public final class RecurrencePattern {
    private final Set<DayOfWeek> daysOfWeek;
    private final LocalTime slotStartTime;
    private final int durationMinutes;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public RecurrencePattern(
            Set<DayOfWeek> daysOfWeek,
            LocalTime slotStartTime,
            int durationMinutes,
            LocalDate startDate,
            LocalDate endDate) {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            throw new IllegalArgumentException("At least one DayOfWeek must be specified for recurring bookings");
        }
        Objects.requireNonNull(slotStartTime, "slotStartTime is required");
        Objects.requireNonNull(startDate, "startDate is required");
        Objects.requireNonNull(endDate, "endDate is required");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        if (Duration.between(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()).toDays() > 90) {
            throw new IllegalArgumentException("Recurring booking horizon cannot exceed 90 days in a single series");
        }
        if (durationMinutes < 30 || durationMinutes > 720) {
            throw new IllegalArgumentException("durationMinutes must be between 30 and 720");
        }
        this.daysOfWeek = Collections.unmodifiableSet(EnumSet.copyOf(daysOfWeek));
        this.slotStartTime = slotStartTime;
        this.durationMinutes = durationMinutes;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Set<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }

    public LocalTime getSlotStartTime() {
        return slotStartTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Expands the recurrence rule into concrete TimeSlot instances.
     */
    public List<TimeSlot> generateOccurrences() {
        List<TimeSlot> slots = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            if (daysOfWeek.contains(cursor.getDayOfWeek())) {
                LocalDateTime start = LocalDateTime.of(cursor, slotStartTime);
                LocalDateTime end = start.plusMinutes(durationMinutes);
                slots.add(new TimeSlot(start, end));
            }
            cursor = cursor.plusDays(1);
        }
        if (slots.isEmpty()) {
            throw new IllegalArgumentException("No matching dates found for the specified RecurrencePattern between "
                    + startDate + " and " + endDate);
        }
        return slots;
    }
}
