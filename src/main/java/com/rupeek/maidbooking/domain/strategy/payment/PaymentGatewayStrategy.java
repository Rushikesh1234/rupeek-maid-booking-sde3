package com.rupeek.maidbooking.domain.strategy.payment;

import com.rupeek.maidbooking.domain.model.PaymentMethodType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Common abstraction for external payment methods (Card, UPI, Wallet).
 * New payment methods (e.g., NET_BANKING, BNPL) can be plugged in by implementing this interface.
 */
public interface PaymentGatewayStrategy {
    PaymentMethodType getSupportedMethod();

    String charge(UUID bookingId, BigDecimal amount, String paymentInstrumentDetails);

    String refund(String originalGatewayReference, BigDecimal refundAmount);
}
