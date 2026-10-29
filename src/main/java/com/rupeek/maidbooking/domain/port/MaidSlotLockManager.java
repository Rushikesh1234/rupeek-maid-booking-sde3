package com.rupeek.maidbooking.domain.port;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Concurrency abstraction for serializing simultaneous booking attempts on the same maid's inventory.
 * In single-node / in-memory mode: backed by fine-grained per-maid ReentrantLocks.
 * In distributed PostgreSQL mode: backed by pg_advisory_xact_lock(hashtext(maid_id)) + GiST exclusion constraint.
 */
public interface MaidSlotLockManager {
    <T> T executeWithMaidLock(UUID maidId, Supplier<T> criticalSection);
}
