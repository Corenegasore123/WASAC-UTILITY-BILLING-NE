package com.ne.wasac.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;

/**
 * Validates that a provided date of birth implies an age of at least the configured minimum.
 */
public class MinimumAgeValidator implements ConstraintValidator<MinimumAge, LocalDate> {

    private int minimumAge;

    /** Reads the minimum age threshold from the annotation. */
    @Override
    public void initialize(MinimumAge constraintAnnotation) {
        this.minimumAge = constraintAnnotation.value();
    }

    /**
     * Skips validation when DOB is null; otherwise enforces age >= minimumAge.
     */
    @Override
    public boolean isValid(LocalDate dateOfBirth, ConstraintValidatorContext context) {
        if (dateOfBirth == null) {
            return true;
        }
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        return age >= minimumAge;
    }
}
