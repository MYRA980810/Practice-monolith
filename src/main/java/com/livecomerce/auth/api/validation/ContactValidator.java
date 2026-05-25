package com.livecomerce.auth.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class ContactValidator implements ConstraintValidator<ValidContact, String> {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    public static boolean isPhone(String contact) {
        return PHONE.matcher(contact).matches();
    }

    public static boolean isEmail(String contact) {
        return EMAIL.matcher(contact).matches();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null || value.isBlank()) return false;
        return EMAIL.matcher(value).matches() || PHONE.matcher(value).matches();
    }
}
