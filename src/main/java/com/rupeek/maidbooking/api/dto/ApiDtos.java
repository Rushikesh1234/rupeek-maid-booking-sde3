package com.rupeek.maidbooking.api.dto;

import com.rupeek.maidbooking.domain.model.BookingStatus;
import com.rupeek.maidbooking.domain.model.BookingType;
import com.rupeek.maidbooking.domain.model.Gender;
import com.rupeek.maidbooking.domain.model.OccurrenceStatus;
import com.rupeek.maidbooking.domain.model.PaymentMethodType;
import com.rupeek.maidbooking.domain.model.PaymentStatus;
import com.rupeek.maidbooking.domain.model.ServiceSkill;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Clean REST API Data Transfer Objects (Frontend API Contract).
 * Keeps domain models decoupled from JSON serialization details.
 */
public final class ApiDtos {

    private ApiDtos() {}

    // --- Maid Onboarding & Discovery DTOs ---

    public record AvailabilityWindowDto(
            @NotNull DayOfWeek dayOfWeek,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime
    ) {}

    public record CreateMaidRequest(
            @NotBlank String name,
            @NotNull Gender gender,
            @NotBlank String locality,
            @NotBlank String city,
            String pincode,
            double latitude,
            double longitude,
            @NotEmpty Set<ServiceSkill> skills,
            @NotNull @DecimalMin("1.0") BigDecimal baseHourlyRate,
            @Min(0) @Max(5) double rating,
            @NotEmpty List<AvailabilityWindowDto> availabilityWindows
    ) {}

    public record MaidResponse(
            UUID id,
            String name,
            Gender gender,
            String locality,
            String city,
            String pincode,
            Set<ServiceSkill> skills,
            BigDecimal baseHourlyRate,
            double rating,
            List<AvailabilityWindowDto> availabilityWindows,
            boolean active
    ) {}

    public record MaidSearchRequest(
            String locality,
            ServiceSkill requiredSkill,
            @NotNull LocalDateTime startTime,
            @NotNull LocalDateTime endTime,
            Double minRating,
            BigDecimal maxHourlyRate,
            Gender genderPreference
    ) {}

    // --- Booking DTOs ---

    public record RecurrencePatternDto(
            @NotEmpty Set<DayOfWeek> daysOfWeek,
            @NotNull LocalTime slotStartTime,
            @Min(30) @Max(720) int durationMinutes,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate
    ) {}

    public record CreateBookingRequest(
            @NotNull UUID customerId,
            @NotNull UUID maidId,
            @NotNull ServiceSkill serviceSkill,
            @NotNull BookingType bookingType,
            Integer instantDurationMinutes,
            LocalDateTime scheduledStartTime,
            LocalDateTime scheduledEndTime,
            RecurrencePatternDto recurrencePattern
    ) {}

    public record BookingOccurrenceDto(
            UUID occurrenceId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BigDecimal amount,
            OccurrenceStatus status,
            LocalDateTime cancelledAt
    ) {}

    public record BookingResponse(
            UUID bookingId,
            UUID customerId,
            UUID maidId,
            ServiceSkill serviceSkill,
            BookingType bookingType,
            BookingStatus status,
            BigDecimal totalAmount,
            List<BookingOccurrenceDto> occurrences,
            LocalDateTime createdAt
    ) {}

    // --- Payment DTOs ---

    public record CreatePaymentRequest(
            @NotNull UUID bookingId,
            UUID occurrenceId,
            @NotNull PaymentMethodType paymentMethod,
            @NotBlank String instrumentDetails
    ) {}

    public record PaymentResponse(
            UUID paymentId,
            UUID bookingId,
            UUID occurrenceId,
            PaymentMethodType paymentMethod,
            BigDecimal amount,
            BigDecimal refundedAmount,
            PaymentStatus status,
            String idempotencyKey,
            String gatewayReference,
            LocalDateTime createdAt
    ) {}

    // --- Cancellation DTOs ---

    public record CancellationResponse(
            UUID bookingId,
            BookingStatus bookingStatus,
            List<UUID> cancelledOccurrenceIds,
            BigDecimal refundAmount,
            int refundPercentage,
            String refundPolicyReason,
            String gatewayRefundReference
    ) {}

    // --- Standardized Error Contract ---

    public record ApiErrorResponse(
            String errorCode,
            String message,
            int httpStatus,
            LocalDateTime timestamp
    ) {}
}
