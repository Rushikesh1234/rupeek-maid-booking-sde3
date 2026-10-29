package com.rupeek.maidbooking.domain.specification;

import com.rupeek.maidbooking.domain.model.Maid;

/**
 * Specification Pattern contract for filtering maids during discovery.
 * Open for extension, closed for modification (SOLID - OCP):
 * To introduce a new filter (e.g., LanguagesSpokenSpecification, VerifiedBackgroundCheckSpecification),
 * simply implement this interface and register it as a Spring @Component.
 */
public interface MaidSpecification {
    boolean isSatisfiedBy(Maid maid, MaidSearchCriteria criteria);
}
