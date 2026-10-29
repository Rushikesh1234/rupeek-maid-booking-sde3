package com.rupeek.maidbooking.infrastructure.persistence;

import com.rupeek.maidbooking.domain.exception.SlotConflictException;
import com.rupeek.maidbooking.domain.port.MaidSlotLockManager;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Fine-grained per-maid lock manager using ReentrantLock with a 3-second acquisition timeout.
 * Bookings for different maids execute with 100% parallelism; simultaneous requests for the
 * SAME maid are serialized safely to prevent double-booking race conditions.
 */
@Component
public class StripedMaidSlotLockManager implements MaidSlotLockManager {

    private static final long LOCK_TIMEOUT_MS = 3000L;
    private final ConcurrentHashMap<UUID, ReentrantLock> maidLocks = new ConcurrentHashMap<>();

    @Override
    public <T> T executeWithMaidLock(UUID maidId, Supplier<T> criticalSection) {
        ReentrantLock lock = maidLocks.computeIfAbsent(maidId, ignored -> new ReentrantLock(true));
        boolean acquired = false;
        try {
            acquired = lock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new SlotConflictException(
                        "High concurrency contention on maid " + maidId + "; please retry shortly");
            }
            return criticalSection.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SlotConflictException("Interrupted while acquiring inventory lock for maid " + maidId);
        } finally {
            if (acquired) {
                lock.unlock();
            }
        }
    }
}
