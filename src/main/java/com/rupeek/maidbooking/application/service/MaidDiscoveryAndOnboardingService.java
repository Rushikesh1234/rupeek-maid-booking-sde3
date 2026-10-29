package com.rupeek.maidbooking.application.service;

import com.rupeek.maidbooking.domain.exception.ResourceNotFoundException;
import com.rupeek.maidbooking.domain.model.Maid;
import com.rupeek.maidbooking.domain.port.BookingRepository;
import com.rupeek.maidbooking.domain.port.MaidRepository;
import com.rupeek.maidbooking.domain.specification.MaidSearchCriteria;
import com.rupeek.maidbooking.domain.specification.MaidSpecification;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Application service for onboarding maids and discovering available maids.
 * Evaluates all registered MaidSpecification beans (Open-Closed filter chain)
 * and verifies real-time slot availability against active BookingOccurrences.
 */
@Service
public class MaidDiscoveryAndOnboardingService {

    private final MaidRepository maidRepository;
    private final BookingRepository bookingRepository;
    private final List<MaidSpecification> specifications;

    public MaidDiscoveryAndOnboardingService(
            MaidRepository maidRepository,
            BookingRepository bookingRepository,
            List<MaidSpecification> specifications) {
        this.maidRepository = maidRepository;
        this.bookingRepository = bookingRepository;
        this.specifications = List.copyOf(specifications);
    }

    public Maid onboardMaid(Maid maid) {
        return maidRepository.save(maid);
    }

    public Maid getMaidById(UUID maidId) {
        return maidRepository.findById(maidId)
                .orElseThrow(() -> new ResourceNotFoundException("Maid not found with id: " + maidId));
    }

    /**
     * Discovers maids matching all filter specifications AND having zero overlapping
     * active bookings for the requested time slot. Results are ranked by rating DESC, hourlyRate ASC.
     */
    public List<Maid> searchAvailableMaids(MaidSearchCriteria criteria) {
        return maidRepository.findAll().stream()
                .filter(maid -> matchesAllSpecifications(maid, criteria))
                .filter(maid -> isSlotFree(maid.getId(), criteria))
                .sorted(Comparator.comparingDouble(Maid::getRating).reversed()
                        .thenComparing(Maid::getBaseHourlyRate))
                .toList();
    }

    private boolean matchesAllSpecifications(Maid maid, MaidSearchCriteria criteria) {
        for (MaidSpecification spec : specifications) {
            if (!spec.isSatisfiedBy(maid, criteria)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSlotFree(UUID maidId, MaidSearchCriteria criteria) {
        return bookingRepository
                .findConflictingOccurrences(maidId, List.of(criteria.requestedSlot()))
                .isEmpty();
    }
}
