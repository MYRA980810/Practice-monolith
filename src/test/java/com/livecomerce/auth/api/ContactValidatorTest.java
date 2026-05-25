package com.livecomerce.auth.api;

import com.livecomerce.auth.api.validation.ContactValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ContactValidatorTest {

    private final ContactValidator validator = new ContactValidator();
    private final ConstraintValidatorContext ctx = mock(ConstraintValidatorContext.class);

    @Test
    void valid_email_passes() {
        assertThat(validator.isValid("user@example.com", ctx)).isTrue();
    }

    @Test
    void valid_email_with_subdomain_passes() {
        assertThat(validator.isValid("user@mail.example.com", ctx)).isTrue();
    }

    @Test
    void valid_e164_phone_passes() {
        assertThat(validator.isValid("+5491112345678", ctx)).isTrue();
    }

    @Test
    void valid_e164_short_phone_passes() {
        assertThat(validator.isValid("+12345678", ctx)).isTrue();
    }

    @Test
    void phone_without_plus_fails() {
        assertThat(validator.isValid("5491112345678", ctx)).isFalse();
    }

    @Test
    void plain_text_no_at_no_plus_fails() {
        assertThat(validator.isValid("notvalid", ctx)).isFalse();
    }

    @Test
    void email_without_domain_fails() {
        assertThat(validator.isValid("user@", ctx)).isFalse();
    }

    @Test
    void email_without_tld_fails() {
        assertThat(validator.isValid("user@domain", ctx)).isFalse();
    }

    @Test
    void blank_fails() {
        assertThat(validator.isValid("", ctx)).isFalse();
    }

    @Test
    void whitespace_fails() {
        assertThat(validator.isValid("   ", ctx)).isFalse();
    }

    @Test
    void null_fails() {
        assertThat(validator.isValid(null, ctx)).isFalse();
    }
}
