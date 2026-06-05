package com.ne.wasac.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * National ID must be exactly 16 numeric characters and unique at service layer.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Size(min = 16, max = 16, message = "National ID must be exactly 16 characters")
@Pattern(regexp = ValidationPatterns.NATIONAL_ID, message = "National ID must be 16 digits")
public @interface NationalId {
    String message() default "Invalid national ID";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
