package com.rupeek.maidbooking.domain.model;

/**
 * Supported booking modes behind the BookingStrategy abstraction.
 */
public enum BookingType {
    INSTANT,
    SCHEDULED,
    RECURRING
}
