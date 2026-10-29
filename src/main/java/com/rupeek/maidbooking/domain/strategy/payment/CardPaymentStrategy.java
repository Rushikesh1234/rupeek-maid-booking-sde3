package com.rupeek.maidbooking.domain.strategy.payment;

import com.rupeek.maidbooking.domain.model.PaymentMethodType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class CardPaymentStrategy implements PaymentGatewayStrategy {

    @Override
    public PaymentMethodType getSupportedMethod() {
        return PaymentMethodType.CARD;
    }

    @Override
    public String charge(UUID bookingId, BigDecimal amount, String paymentInstrumentDetails) {
        if (paymentInstrumentDetails == null || paymentInstrumentDetails.trim().length() < 4) {
            throw new IllegalArgumentException("Card token or masked card number is required for CARD payment");
        }
        return "CRD-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public String refund(String originalGatewayReference, BigDecimal refundAmount) {
        return "CRD-RFD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
