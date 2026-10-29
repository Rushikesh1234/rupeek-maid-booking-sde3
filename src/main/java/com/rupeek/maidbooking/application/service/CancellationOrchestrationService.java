package com.rupeek.maidbooking.application.service;

import com.rupeek.maidbooking.domain.exception.ResourceNotFoundException;
import com.rupeek.maidbooking.domain.model.Booking;
import com.rupeek.maidbooking.domain.model.BookingOccurrence;
import com.rupeek.maidbooking.domain.model.Payment;
import com.rupeek.maidbooking.domain.model.PaymentMethodType;
import com.rupeek.maidbooking.domain.model.PaymentStatus;
import com.rupeek.maidbooking.domain.model.RefundResult;
import com.rupeek.maidbooking.domain.port.BookingRepository;
import com.rupeek.maidbooking.domain.port.MaidSlotLockManager;
import com.rupeek.maidbooking.domain.port.PaymentRepository;
import com.rupeek.maidbooking.domain.strategy.cancellation.CancellationRefundPolicy;
import com.rupeek.maidbooking.domain.strategy.payment.PaymentGatewayStrategy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles full-series and single-occurrence booking cancellations,
 * freeing the maid's slot immediately and triggering policy-driven refunds.
 */
@Service
public class CancellationOrchestrationService {

    public record CancellationOutcome(
            Booking updatedBooking,
            List<BookingOccurrence> cancelledOccurrences,
            RefundResult refundResult
    ) {}

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final MaidSlotLockManager lockManager;
    private final CancellationRefundPolicy refundPolicy;
    private final Map<PaymentMethodType, PaymentGatewayStrategy> gateways;
    private final Clock clock;

    public CancellationOrchestrationService(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            MaidSlotLockManager lockManager,
            CancellationRefundPolicy refundPolicy,
            List<PaymentGatewayStrategy> gatewayStrategies,
            Clock clock) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.lockManager = lockManager;
        this.refundPolicy = refundPolicy;
        this.clock = clock;
        this.gateways = new EnumMap<>(PaymentMethodType.class);
        for (PaymentGatewayStrategy strategy : gatewayStrategies) {
            this.gateways.put(strategy.getSupportedMethod(), strategy);
        }
    }

    /**
     * Cancels an entire booking (or entire recurring series), freeing all active slots.
     */
    public CancellationOutcome cancelEntireBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        return lockManager.executeWithMaidLock(booking.getMaidId(), () -> {
            LocalDateTime now = LocalDateTime.now(clock);
            List<BookingOccurrence> cancelledOccurrences = booking.cancelEntireSeries(now);
            bookingRepository.save(booking);

            RefundResult refundResult = executeRefundIfPaid(booking.getId(), cancelledOccurrences, now);
            return new CancellationOutcome(booking, cancelledOccurrences, refundResult);
        });
    }

    /**
     * Cancels a single occurrence in a booking (designed for RECURRING bookings),
     * freeing that specific slot so the maid is discoverable and bookable again for that time.
     */
    public CancellationOutcome cancelSingleOccurrence(UUID bookingId, UUID occurrenceId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        return lockManager.executeWithMaidLock(booking.getMaidId(), () -> {
            LocalDateTime now = LocalDateTime.now(clock);
            BookingOccurrence cancelled = booking.cancelSingleOccurrence(occurrenceId, now);
            bookingRepository.save(booking);

            RefundResult refundResult = executeRefundIfPaid(booking.getId(), List.of(cancelled), now);
            return new CancellationOutcome(booking, List.of(cancelled), refundResult);
        });
    }

    private RefundResult executeRefundIfPaid(
            UUID bookingId,
            List<BookingOccurrence> cancelledOccurrences,
            LocalDateTime cancelledAt) {
        List<Payment> payments = paymentRepository.findByBookingId(bookingId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS || p.getStatus() == PaymentStatus.PARTIALLY_REFUNDED)
                .toList();

        if (payments.isEmpty()) {
            return new RefundResult(
                    BigDecimal.ZERO,
                    0,
                    "Booking was unpaid (PENDING_PAYMENT); slot freed with no refund required",
                    "N/A");
        }

        CancellationRefundPolicy.RefundEvaluation evaluation =
                refundPolicy.evaluate(cancelledOccurrences, cancelledAt);

        BigDecimal remainingToRefund = evaluation.refundableAmount();
        String lastGatewayRefundId = "NO-REFUND-DUE";

        if (remainingToRefund.compareTo(BigDecimal.ZERO) > 0) {
            for (Payment payment : payments) {
                BigDecimal available = payment.getRefundableBalance();
                if (available.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal refundSlice = remainingToRefund.min(available);
                PaymentGatewayStrategy gateway = gateways.get(payment.getPaymentMethod());
                lastGatewayRefundId = gateway.refund(payment.getGatewayReference(), refundSlice);
                payment.recordRefund(refundSlice);
                paymentRepository.save(payment);
                remainingToRefund = remainingToRefund.subtract(refundSlice);
                if (remainingToRefund.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
            }
        }

        return new RefundResult(
                evaluation.refundableAmount().subtract(remainingToRefund),
                evaluation.refundPercentage(),
                evaluation.reason(),
                lastGatewayRefundId);
    }
}
