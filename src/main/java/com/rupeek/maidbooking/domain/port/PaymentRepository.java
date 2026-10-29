package com.rupeek.maidbooking.domain.port;

import com.rupeek.maidbooking.domain.model.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for Payments, supporting idempotency lookup by idempotencyKey.
 */
public interface PaymentRepository {
    Payment save(Payment payment);

    Optional<Payment> findById(UUID paymentId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByBookingId(UUID bookingId);
}
