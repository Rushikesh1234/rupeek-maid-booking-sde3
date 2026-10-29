package com.rupeek.maidbooking.domain.specification;

import com.rupeek.maidbooking.domain.model.Gender;
import com.rupeek.maidbooking.domain.model.ServiceSkill;
import com.rupeek.maidbooking.domain.model.TimeSlot;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Encapsulates all optional and required discovery filters.
 * New filter fields can be added and paired with a MaidSpecification bean
 * without changing the search orchestration logic.
 */
public record MaidSearchCriteria(
        String locality,
        ServiceSkill requiredSkill,
        TimeSlot requestedSlot,
        Double minRating,
        BigDecimal maxHourlyRate,
        Gender genderPreference
) {
    public MaidSearchCriteria {
        Objects.requireNonNull(requestedSlot, "requestedSlot is required to verify maid availability");
    }
}
