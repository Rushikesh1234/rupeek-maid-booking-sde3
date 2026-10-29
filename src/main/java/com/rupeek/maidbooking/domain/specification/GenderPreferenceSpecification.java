package com.rupeek.maidbooking.domain.specification;

import com.rupeek.maidbooking.domain.model.Gender;
import com.rupeek.maidbooking.domain.model.Maid;
import org.springframework.stereotype.Component;

@Component
public class GenderPreferenceSpecification implements MaidSpecification {
    @Override
    public boolean isSatisfiedBy(Maid maid, MaidSearchCriteria criteria) {
        if (criteria.genderPreference() == null || criteria.genderPreference() == Gender.ANY) {
            return true;
        }
        return maid.getGender() == criteria.genderPreference();
    }
}
