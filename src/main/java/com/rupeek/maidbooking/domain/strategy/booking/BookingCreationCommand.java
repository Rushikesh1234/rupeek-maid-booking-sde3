package com.rupeek.maidbooking.domain.strategy.booking;

import com.rupeek.maidbooking.domain.model.BookingType;
import com.rupeek.maidbooking.domain.model.RecurrencePattern;
import com.rupeek.maidbooking.domain.model.ServiceSkill;
import com.rupeek.maidbooking.domain.model.TimeSlot;

import java.util.UUID;

/**
 * Command object passed into the BookingOrchestrationService and BookingStrategy implementations.
 */
public record BookingCreationCommand(
        UUID customerId,
        UUID maidId,
        ServiceSkill serviceSkill,
        BookingType bookingType,
        Integer instantDurationMinutes,
        TimeSlot scheduledSlot,
        RecurrencePattern recurrencePattern
) {}
