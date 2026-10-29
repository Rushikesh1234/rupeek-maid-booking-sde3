-- ============================================================================
-- Rupeek SDE-3 Maid Booking System — PostgreSQL 16 Production Schema
-- Enforces ACID guarantees, GiST Exclusion Constraints against double-booking,
-- and Idempotency Keys for payment safety.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- 1. Maids Master Table
CREATE TABLE IF NOT EXISTS maids (
    id                  UUID PRIMARY KEY,
    name                VARCHAR(120) NOT NULL,
    gender              VARCHAR(20)  NOT NULL,
    locality            VARCHAR(100) NOT NULL,
    city                VARCHAR(80)  NOT NULL,
    pincode             VARCHAR(12)  NOT NULL,
    latitude            DOUBLE PRECISION NOT NULL,
    longitude           DOUBLE PRECISION NOT NULL,
    base_hourly_rate    NUMERIC(10, 2) NOT NULL CHECK (base_hourly_rate > 0),
    rating              NUMERIC(3, 2)  NOT NULL DEFAULT 5.00 CHECK (rating >= 0.0 AND rating <= 5.0),
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_maids_locality_active_rating
    ON maids (LOWER(locality), active, rating DESC, base_hourly_rate ASC);

-- 2. Maid Skills Table
CREATE TABLE IF NOT EXISTS maid_skills (
    maid_id             UUID NOT NULL REFERENCES maids(id) ON DELETE CASCADE,
    skill               VARCHAR(40) NOT NULL,
    PRIMARY KEY (maid_id, skill)
);

CREATE INDEX IF NOT EXISTS idx_maid_skills_skill ON maid_skills (skill);

-- 3. Maid Weekly Availability Windows
CREATE TABLE IF NOT EXISTS maid_availability_windows (
    id                  UUID PRIMARY KEY,
    maid_id             UUID NOT NULL REFERENCES maids(id) ON DELETE CASCADE,
    day_of_week         VARCHAR(15) NOT NULL,
    start_time          TIME NOT NULL,
    end_time            TIME NOT NULL,
    CHECK (end_time > start_time)
);

CREATE INDEX IF NOT EXISTS idx_maid_avail_maid_day
    ON maid_availability_windows (maid_id, day_of_week);

-- 4. Bookings Aggregate Header Table
CREATE TABLE IF NOT EXISTS bookings (
    id                  UUID PRIMARY KEY,
    customer_id         UUID NOT NULL,
    maid_id             UUID NOT NULL REFERENCES maids(id),
    service_skill       VARCHAR(40) NOT NULL,
    booking_type        VARCHAR(30) NOT NULL,
    status              VARCHAR(30) NOT NULL,
    total_amount        NUMERIC(10, 2) NOT NULL CHECK (total_amount >= 0),
    recurrence_rule     VARCHAR(255),
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bookings_customer_created
    ON bookings (customer_id, created_at DESC);

-- 5. Booking Occurrences Table (Individual Time Slots)
-- Uses PostgreSQL GiST Exclusion Constraint on tsrange(start_time, end_time)
-- so two non-cancelled occurrences for the same maid_id CANNOT overlap at the DB level!
CREATE TABLE IF NOT EXISTS booking_occurrences (
    id                  UUID PRIMARY KEY,
    booking_id          UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    maid_id             UUID NOT NULL REFERENCES maids(id),
    start_time          TIMESTAMPTZ NOT NULL,
    end_time            TIMESTAMPTZ NOT NULL,
    status              VARCHAR(30) NOT NULL,
    occurrence_amount   NUMERIC(10, 2) NOT NULL CHECK (occurrence_amount >= 0),
    cancelled_at        TIMESTAMPTZ,
    CHECK (end_time > start_time),
    CONSTRAINT no_overlapping_active_maid_slots
        EXCLUDE USING gist (
            maid_id WITH =,
            tstzrange(start_time, end_time, '[)') WITH &&
        ) WHERE (status IN ('PENDING_PAYMENT', 'CONFIRMED'))
);

CREATE INDEX IF NOT EXISTS idx_occurrences_maid_window
    ON booking_occurrences (maid_id, start_time, end_time)
    WHERE status IN ('PENDING_PAYMENT', 'CONFIRMED');

-- 6. Payments Table (Idempotent Payment Ledger)
CREATE TABLE IF NOT EXISTS payments (
    id                  UUID PRIMARY KEY,
    booking_id          UUID NOT NULL REFERENCES bookings(id),
    occurrence_id       UUID REFERENCES booking_occurrences(id),
    payment_method      VARCHAR(30) NOT NULL,
    amount              NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    refunded_amount     NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (refunded_amount >= 0),
    status              VARCHAR(30) NOT NULL,
    idempotency_key     VARCHAR(120) NOT NULL UNIQUE,
    gateway_reference   VARCHAR(120) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payments_booking ON payments (booking_id);
