package com.rupeek.maidbooking.api.controller;

import com.rupeek.maidbooking.api.dto.ApiDtos.CreatePaymentRequest;
import com.rupeek.maidbooking.api.dto.ApiDtos.PaymentResponse;
import com.rupeek.maidbooking.application.service.PaymentOrchestrationService;
import com.rupeek.maidbooking.application.service.PaymentOrchestrationService.PaymentCommand;
import com.rupeek.maidbooking.domain.model.Payment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "3. Payments", description = "Idempotent payment processing across UPI, Card, and Wallet")
public class PaymentController {

    private final PaymentOrchestrationService paymentService;

    public PaymentController(PaymentOrchestrationService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(summary = "Take payment for a full booking or a single recurring occurrence (Idempotent via Idempotency-Key header)")
    public ResponseEntity<PaymentResponse> payForBooking(
            @Parameter(description = "Unique client-generated key to guarantee idempotent payment execution", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        Payment payment = paymentService.processPayment(new PaymentCommand(
                request.bookingId(),
                request.occurrenceId(),
                request.paymentMethod(),
                request.instrumentDetails(),
                idempotencyKey));

        return ResponseEntity.ok(new PaymentResponse(
                payment.getId(),
                payment.getBookingId(),
                payment.getOccurrenceId(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getRefundedAmount(),
                payment.getStatus(),
                payment.getIdempotencyKey(),
                payment.getGatewayReference(),
                payment.getCreatedAt()));
    }
}
