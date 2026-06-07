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
 * Rwanda mobile: local 072xxxxxxx (10 digits) or international +25072xxxxxxx (13 chars).
 * Valid prefixes after 0 or +250: 72, 73, 78, 79.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Pattern(regexp = ValidationPatterns.PHONE_NUMBER,
        message = "Phone must be 10 digits (072/073/078/079xxxxxxx) or 13 chars (+25072/073/078/079xxxxxxx)")
public @interface PhoneNumber {
    String message() default "Phone must be 10 digits (072/073/078/079xxxxxxx) or 13 chars (+25072/073/078/079xxxxxxx)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
