package com.rupeek.maidbooking.domain.strategy.payment;

import com.rupeek.maidbooking.domain.model.PaymentMethodType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class UpiPaymentStrategy implements PaymentGatewayStrategy {

    @Override
    public PaymentMethodType getSupportedMethod() {
        return PaymentMethodType.UPI;
    }

    @Override
    public String charge(UUID bookingId, BigDecimal amount, String paymentInstrumentDetails) {
        if (paymentInstrumentDetails == null || !paymentInstrumentDetails.contains("@")) {
            throw new IllegalArgumentException("Valid UPI VPA (e.g., user@okicici) is required for UPI payment");
        }
        return "UPI-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public String refund(String originalGatewayReference, BigDecimal refundAmount) {
        return "UPI-RFD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
