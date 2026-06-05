package com.ne.wasac.validation;

import com.ne.wasac.dto.MeterReadingRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Enforces consumption = current - previous is strictly positive at DTO level.
 */
public class MeterReadingValidator implements ConstraintValidator<ValidMeterReading, MeterReadingRequest> {

    /** Returns false when current reading is not greater than previous. */
    @Override
    public boolean isValid(MeterReadingRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getPreviousReading() == null || request.getCurrentReading() == null) {
            return true;
        }
        return request.getCurrentReading().compareTo(request.getPreviousReading()) > 0;
    }
}
