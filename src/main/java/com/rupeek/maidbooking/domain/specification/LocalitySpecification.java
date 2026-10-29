package com.rupeek.maidbooking.domain.specification;

import com.rupeek.maidbooking.domain.model.Maid;
import org.springframework.stereotype.Component;

@Component
public class LocalitySpecification implements MaidSpecification {
    @Override
    public boolean isSatisfiedBy(Maid maid, MaidSearchCriteria criteria) {
        return maid.isActive() && maid.getLocation().matchesLocality(criteria.locality());
    }
}
