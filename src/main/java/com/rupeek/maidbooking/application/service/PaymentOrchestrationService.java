package com.rupeek.maidbooking.application.service;

import com.rupeek.maidbooking.domain.exception.ResourceNotFoundException;
import com.rupeek.maidbooking.domain.model.Booking;
import com.rupeek.maidbooking.domain.model.BookingOccurrence;
import com.rupeek.maidbooking.domain.model.Payment;
import com.rupeek.maidbooking.domain.model.PaymentMethodType;
import com.rupeek.maidbooking.domain.model.PaymentStatus;
import com.rupeek.maidbooking.domain.port.BookingRepository;
import com.rupeek.maidbooking.domain.port.PaymentRepository;
import com.rupeek.maidbooking.domain.strategy.payment.PaymentGatewayStrategy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates booking payments with strict Idempotency-Key deduplication
 * and supports both full-booking payment and per-occurrence recurring payment.
 */
@Service
public class PaymentOrchestrationService {

    public record PaymentCommand(
            UUID bookingId,
            UUID occurrenceId,
            PaymentMethodType paymentMethod,
            String instrumentDetails,
            String idempotencyKey
    ) {}

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final Map<PaymentMethodType, PaymentGatewayStrategy> gateways;
    private final Clock clock;

    public PaymentOrchestrationService(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            List<PaymentGatewayStrategy> gatewayStrategies,
            Clock clock) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.clock = clock;
        this.gateways = new EnumMap<>(PaymentMethodType.class);
        for (PaymentGatewayStrategy strategy : gatewayStrategies) {
            this.gateways.put(strategy.getSupportedMethod(), strategy);
        }
    }

    public synchronized Payment processPayment(PaymentCommand command) {
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key is required for payment processing");
        }

        // 1. Idempotency Check: Return existing payment if idempotencyKey was already processed
        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(command.idempotencyKey().trim());
        if (existingPayment.isPresent()) {
            return existingPayment.get();
        }

        Booking booking = bookingRepository.findById(command.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + command.bookingId()));

        PaymentGatewayStrategy gateway = gateways.get(command.paymentMethod());
        if (gateway == null) {
            throw new IllegalArgumentException("Unsupported payment method: " + command.paymentMethod());
        }

        BigDecimal chargeAmount;
        if (command.occurrenceId() != null) {
            // Per-occurrence payment (specifically useful for RECURRING bookings)
            BookingOccurrence occurrence = booking.findOccurrence(command.occurrenceId());
            chargeAmount = occurrence.getAmount();
            String gatewayRef = gateway.charge(booking.getId(), chargeAmount, command.instrumentDetails());
            booking.confirmSingleOccurrence(command.occurrenceId());
            bookingRepository.save(booking);

            Payment payment = new Payment(
                    UUID.randomUUID(),
                    booking.getId(),
                    command.occurrenceId(),
                    command.paymentMethod(),
                    chargeAmount,
                    command.idempotencyKey().trim(),
                    gatewayRef,
                    PaymentStatus.SUCCESS,
                    BigDecimal.ZERO,
                    LocalDateTime.now(clock));
            return paymentRepository.save(payment);
        } else {
            // Full booking payment
            chargeAmount = booking.getTotalAmount();
            String gatewayRef = gateway.charge(booking.getId(), chargeAmount, command.instrumentDetails());
            booking.confirmAllOccurrences();
            bookingRepository.save(booking);

            Payment payment = new Payment(
                    UUID.randomUUID(),
                    booking.getId(),
                    null,
                    command.paymentMethod(),
                    chargeAmount,
                    command.idempotencyKey().trim(),
                    gatewayRef,
                    PaymentStatus.SUCCESS,
                    BigDecimal.ZERO,
                    LocalDateTime.now(clock));
            return paymentRepository.save(payment);
        }
    }
}
