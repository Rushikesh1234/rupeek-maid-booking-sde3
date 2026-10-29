package com.rupeek.maidbooking.infrastructure.persistence;

import com.rupeek.maidbooking.domain.model.Payment;
import com.rupeek.maidbooking.domain.port.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<UUID, Payment> paymentsById = new ConcurrentHashMap<>();
    private final Map<String, Payment> paymentsByIdempotencyKey = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        paymentsById.put(payment.getId(), payment);
        paymentsByIdempotencyKey.put(payment.getIdempotencyKey(), payment);
        return payment;
    }

    @Override
    public Optional<Payment> findById(UUID paymentId) {
        return Optional.ofNullable(paymentsById.get(paymentId));
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(paymentsByIdempotencyKey.get(idempotencyKey));
    }

    @Override
    public List<Payment> findByBookingId(UUID bookingId) {
        return paymentsById.values().stream()
                .filter(p -> p.getBookingId().equals(bookingId))
                .toList();
    }
}
