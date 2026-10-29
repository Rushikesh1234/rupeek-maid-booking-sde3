package com.rupeek.maidbooking.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Rich Domain Entity representing a Maid onboarded onto the platform.
 */
public class Maid {
    private final UUID id;
    private final String name;
    private final Gender gender;
    private final Location location;
    private final Set<ServiceSkill> skills;
    private final BigDecimal baseHourlyRate;
    private final double rating;
    private final List<AvailabilityWindow> availabilityWindows;
    private final boolean active;

    public Maid(
            UUID id,
            String name,
            Gender gender,
            Location location,
            Set<ServiceSkill> skills,
            BigDecimal baseHourlyRate,
            double rating,
            List<AvailabilityWindow> availabilityWindows,
            boolean active) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Maid name must not be blank");
        }
        if (skills == null || skills.isEmpty()) {
            throw new IllegalArgumentException("Maid must offer at least one ServiceSkill");
        }
        if (baseHourlyRate == null || baseHourlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("baseHourlyRate must be positive");
        }
        if (rating < 0.0 || rating > 5.0) {
            throw new IllegalArgumentException("rating must be between 0.0 and 5.0");
        }
        if (availabilityWindows == null || availabilityWindows.isEmpty()) {
            throw new IllegalArgumentException("Maid must specify at least one AvailabilityWindow");
        }
        this.id = id != null ? id : UUID.randomUUID();
        this.name = name.trim();
        this.gender = Objects.requireNonNull(gender, "gender is required");
        this.location = Objects.requireNonNull(location, "location is required");
        this.skills = Collections.unmodifiableSet(EnumSet.copyOf(skills));
        this.baseHourlyRate = baseHourlyRate;
        this.rating = rating;
        this.availabilityWindows = Collections.unmodifiableList(new ArrayList<>(availabilityWindows));
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Gender getGender() {
        return gender;
    }

    public Location getLocation() {
        return location;
    }

    public Set<ServiceSkill> getSkills() {
        return skills;
    }

    public BigDecimal getBaseHourlyRate() {
        return baseHourlyRate;
    }

    public double getRating() {
        return rating;
    }

    public List<AvailabilityWindow> getAvailabilityWindows() {
        return availabilityWindows;
    }

    public boolean isActive() {
        return active;
    }

    public boolean hasSkill(ServiceSkill skill) {
        return skill == null || skills.contains(skill);
    }

    /**
     * Checks whether the maid's shift schedule covers the requested TimeSlot.
     * Note: Actual busy-slot conflict detection is checked against active BookingOccurrences.
     */
    public boolean isWithinWorkingHours(TimeSlot requestedSlot) {
        return availabilityWindows.stream().anyMatch(window -> window.covers(requestedSlot));
    }
}
