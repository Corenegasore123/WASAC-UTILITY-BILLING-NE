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
 * Ensures a name field contains letters and spaces only (no digits).
 * Enforces the WASAC rule that person names must not include numbers.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Pattern(regexp = ValidationPatterns.LETTERS_ONLY_NAME,
        message = "Name must contain letters only, no numbers")
public @interface LettersOnly {
    String message() default "Name must contain letters only, no numbers";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
