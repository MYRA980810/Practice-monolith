package com.livecomerce.auth.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = ContactValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidContact {
    String message() default "Must be a valid email address or E.164 phone number (e.g. +5491112345678)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
