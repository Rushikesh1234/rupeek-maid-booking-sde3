package com.rupeek.maidbooking.domain.strategy.payment;

import com.rupeek.maidbooking.domain.model.PaymentMethodType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class WalletPaymentStrategy implements PaymentGatewayStrategy {

    @Override
    public PaymentMethodType getSupportedMethod() {
        return PaymentMethodType.WALLET;
    }

    @Override
    public String charge(UUID bookingId, BigDecimal amount, String paymentInstrumentDetails) {
        return "WLT-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public String refund(String originalGatewayReference, BigDecimal refundAmount) {
        return "WLT-RFD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
