package com.rupeek.maidbooking.api.exception;

import com.rupeek.maidbooking.api.dto.ApiDtos.ApiErrorResponse;
import com.rupeek.maidbooking.domain.exception.InvalidStateTransitionException;
import com.rupeek.maidbooking.domain.exception.PaymentProcessingException;
import com.rupeek.maidbooking.domain.exception.ResourceNotFoundException;
import com.rupeek.maidbooking.domain.exception.SlotConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(SlotConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleSlotConflict(SlotConflictException ex) {
        return build(HttpStatus.CONFLICT, "SLOT_ALREADY_BOOKED", ex.getMessage());
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidState(InvalidStateTransitionException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_STATE_TRANSITION", ex.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, PaymentProcessingException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException ex) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "PAYLOAD_VALIDATION_FAILED", details);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(
                new ApiErrorResponse(code, message, status.value(), LocalDateTime.now()));
    }
}
