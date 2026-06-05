package com.ne.wasac.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meter numbers must be WM-##### for water or EM-##### for electricity.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Pattern(regexp = ValidationPatterns.METER_NUMBER, message = "Format must be WM-XXXXX or EM-XXXXX")
public @interface MeterNumberFormat {
    String message() default "Format must be WM-XXXXX or EM-XXXXX";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
