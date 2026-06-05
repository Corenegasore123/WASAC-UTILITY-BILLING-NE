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
 * Local or international phone: 7–15 digits, optional leading + country code.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Pattern(regexp = ValidationPatterns.PHONE_NUMBER,
        message = "Phone must be a valid local or international number (7–15 digits, optional + prefix)")
public @interface PhoneNumber {
    String message() default "Phone must be a valid local or international number (7–15 digits, optional + prefix)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
