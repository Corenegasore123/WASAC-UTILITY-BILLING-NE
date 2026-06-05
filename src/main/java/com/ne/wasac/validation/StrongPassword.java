package com.ne.wasac.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Password must be at least 8 characters with uppercase, lowercase, digits, and symbols.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {

    String message() default "Password must be at least 8 characters and include uppercase, lowercase, numbers, and symbols";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
