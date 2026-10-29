package com.rupeek.maidbooking.api.controller;

import com.rupeek.maidbooking.api.dto.ApiDtos.BookingOccurrenceDto;
import com.rupeek.maidbooking.api.dto.ApiDtos.BookingResponse;
import com.rupeek.maidbooking.api.dto.ApiDtos.CancellationResponse;
import com.rupeek.maidbooking.api.dto.ApiDtos.CreateBookingRequest;
import com.rupeek.maidbooking.application.service.BookingOrchestrationService;
import com.rupeek.maidbooking.application.service.CancellationOrchestrationService;
import com.rupeek.maidbooking.application.service.CancellationOrchestrationService.CancellationOutcome;
import com.rupeek.maidbooking.domain.model.Booking;
import com.rupeek.maidbooking.domain.model.BookingOccurrence;
import com.rupeek.maidbooking.domain.model.RecurrencePattern;
import com.rupeek.maidbooking.domain.model.TimeSlot;
import com.rupeek.maidbooking.domain.strategy.booking.BookingCreationCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "2. Bookings & Cancellations", description = "Create Instant, Scheduled, or Recurring bookings and cancel full series or single occurrences")
public class BookingController {

    private final BookingOrchestrationService bookingService;
    private final CancellationOrchestrationService cancellationService;

    public BookingController(
            BookingOrchestrationService bookingService,
            CancellationOrchestrationService cancellationService) {
        this.bookingService = bookingService;
        this.cancellationService = cancellationService;
    }

    @PostMapping
    @Operation(summary = "Create an Instant, Scheduled, or Recurring maid booking with double-booking protection")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        TimeSlot scheduledSlot = null;
        if (request.scheduledStartTime() != null && request.scheduledEndTime() != null) {
            scheduledSlot = new TimeSlot(request.scheduledStartTime(), request.scheduledEndTime());
        }

        RecurrencePattern recurrencePattern = null;
        if (request.recurrencePattern() != null) {
            var r = request.recurrencePattern();
            recurrencePattern = new RecurrencePattern(
                    r.daysOfWeek(),
                    r.slotStartTime(),
                    r.durationMinutes(),
                    r.startDate(),
                    r.endDate());
        }

        BookingCreationCommand command = new BookingCreationCommand(
                request.customerId(),
                request.maidId(),
                request.serviceSkill(),
                request.bookingType(),
                request.instantDurationMinutes(),
                scheduledSlot,
                recurrencePattern);

        Booking booking = bookingService.createBooking(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(booking));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Fetch a booking and all its occurrences by ID")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(toResponse(bookingService.getBookingById(bookingId)));
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel an entire booking (or full recurring series), apply refund policy, and free slots")
    public ResponseEntity<CancellationResponse> cancelEntireBooking(@PathVariable UUID bookingId) {
        CancellationOutcome outcome = cancellationService.cancelEntireBooking(bookingId);
        return ResponseEntity.ok(toCancellationResponse(outcome));
    }

    @PostMapping("/{bookingId}/occurrences/{occurrenceId}/cancel")
    @Operation(summary = "Cancel a single occurrence of a recurring booking, apply refund policy, and free that slot")
    public ResponseEntity<CancellationResponse> cancelSingleOccurrence(
            @PathVariable UUID bookingId,
            @PathVariable UUID occurrenceId) {
        CancellationOutcome outcome = cancellationService.cancelSingleOccurrence(bookingId, occurrenceId);
        return ResponseEntity.ok(toCancellationResponse(outcome));
    }

    private BookingResponse toResponse(Booking booking) {
        List<BookingOccurrenceDto> occurrences = booking.getOccurrences().stream()
                .map(o -> new BookingOccurrenceDto(
                        o.getId(),
                        o.getTimeSlot().getStartTime(),
                        o.getTimeSlot().getEndTime(),
                        o.getAmount(),
                        o.getStatus(),
                        o.getCancelledAt()))
                .toList();
        return new BookingResponse(
                booking.getId(),
                booking.getCustomerId(),
                booking.getMaidId(),
                booking.getServiceSkill(),
                booking.getBookingType(),
                booking.getStatus(),
                booking.getTotalAmount(),
                occurrences,
                booking.getCreatedAt());
    }

    private CancellationResponse toCancellationResponse(CancellationOutcome outcome) {
        List<UUID> ids = outcome.cancelledOccurrences().stream()
                .map(BookingOccurrence::getId)
                .toList();
        return new CancellationResponse(
                outcome.updatedBooking().getId(),
                outcome.updatedBooking().getStatus(),
                ids,
                outcome.refundResult().refundAmount(),
                outcome.refundResult().refundPercentage(),
                outcome.refundResult().policyExplanation(),
                outcome.refundResult().gatewayRefundReference());
    }
}
