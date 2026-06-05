package com.ne.wasac.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When tariff type is TIERED, tiers must not overlap and must be continuous without gaps.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TariffTiersValidator.class)
public @interface ValidTariffTiers {
    String message() default "Tier ranges must be continuous and must not overlap";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
