package com.ne.wasac.validation;

import com.ne.wasac.dto.TariffPlanRequest;
import com.ne.wasac.dto.TariffTierRequest;
import com.ne.wasac.enums.TariffType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Comparator;
import java.util.List;

/**
 * Validates tier continuity and non-overlap for TIERED tariff plans.
 */
public class TariffTiersValidator implements ConstraintValidator<ValidTariffTiers, TariffPlanRequest> {

    /** Skips validation for FLAT tariffs; checks tier ordering for TIERED. */
    @Override
    public boolean isValid(TariffPlanRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getTariffType() != TariffType.TIERED) {
            return true;
        }
        List<TariffTierRequest> tiers = request.getTiers();
        if (tiers == null || tiers.isEmpty()) {
            return reject(context, "At least one tier is required for TIERED tariff");
        }
        tiers.sort(Comparator.comparing(TariffTierRequest::getMinUnit));
        for (TariffTierRequest tier : tiers) {
            if (tier.getMaxUnit() == null || tier.getMinUnit() == null
                    || tier.getMaxUnit().compareTo(tier.getMinUnit()) <= 0) {
                return reject(context, "Each tier maxUnit must be greater than minUnit");
            }
        }
        for (int i = 0; i < tiers.size() - 1; i++) {
            if (tiers.get(i).getMaxUnit().compareTo(tiers.get(i + 1).getMinUnit()) != 0) {
                return reject(context,
                        "Tier ranges must be continuous: next tier minUnit must equal previous tier maxUnit "
                                + "(e.g. 0–100 then 100–500, not 101–500)");
            }
        }
        return true;
    }

    private boolean reject(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
