package com.livecomerce.notification.infrastructure;

import com.livecomerce.notification.application.port.out.SendEmailOtpPort;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
@RequiredArgsConstructor
class ResendEmailAdapter implements SendEmailOtpPort {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailAdapter.class);

    private final Resend resend;

    @Value("${resend.from-address}")
    private String fromAddress;

    @Override
    public void send(String email, String code) {
        var options = CreateEmailOptions.builder()
                .from(fromAddress)
                .to(email)
                .subject("Your verification code")
                .html("<p>Your verification code is: <strong>" + code + "</strong></p>"
                        + "<p>This code expires in 10 minutes.</p>")
                .build();
        try {
            resend.emails().send(options);
        } catch (ResendException e) {
            log.error("Failed to send OTP email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send verification email", e);
        }
    }
}
