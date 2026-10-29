package com.rupeek.maidbooking.domain.specification;

import com.rupeek.maidbooking.domain.model.Maid;
import org.springframework.stereotype.Component;

@Component
public class RatingSpecification implements MaidSpecification {
    @Override
    public boolean isSatisfiedBy(Maid maid, MaidSearchCriteria criteria) {
        if (criteria.minRating() == null) {
            return true;
        }
        return maid.getRating() >= criteria.minRating();
    }
}
