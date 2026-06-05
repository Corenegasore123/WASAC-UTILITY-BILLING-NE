package com.ne.wasac.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level rule: current reading must be strictly greater than previous reading.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MeterReadingValidator.class)
public @interface ValidMeterReading {
    String message() default "Current reading must be greater than previous reading";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
