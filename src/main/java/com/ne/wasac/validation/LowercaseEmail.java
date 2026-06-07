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
 * Valid email format with lowercase letters only — capital letters are rejected.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Pattern(regexp = ValidationPatterns.LOWERCASE_EMAIL,
        message = "Email must be valid and lowercase (no capital letters)")
public @interface LowercaseEmail {
    String message() default "Email must be valid and lowercase (no capital letters)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
