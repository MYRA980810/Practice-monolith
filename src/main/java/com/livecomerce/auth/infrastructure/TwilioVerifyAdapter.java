package com.livecomerce.auth.infrastructure;

import com.livecomerce.auth.application.port.out.VerifyOtpPort;
import com.livecomerce.auth.domain.VerificationChannel;
import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
class TwilioVerifyAdapter implements VerifyOtpPort {

    private static final Logger log = LoggerFactory.getLogger(TwilioVerifyAdapter.class);

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.verify-service-sid}")
    private String serviceSid;

    @PostConstruct
    void init() {
        Twilio.init(accountSid, authToken);
    }

    @Override
    public void send(String to, VerificationChannel channel) {
        String twilioChannel = switch (channel) {
            case WHATSAPP -> "whatsapp";
            case SMS      -> "sms";
            case EMAIL    -> "email";
        };
        try {
            Verification.creator(serviceSid, to, twilioChannel).create();
        } catch (Exception e) {
            log.error("Failed to send Twilio Verify to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send verification code", e);
        }
    }

    @Override
    public boolean check(String to, String code, VerificationChannel channel) {
        try {
            VerificationCheck result = VerificationCheck.creator(serviceSid)
                    .setTo(to)
                    .setCode(code)
                    .create();
            return "approved".equalsIgnoreCase(result.getStatus().toString());
        } catch (Exception e) {
            log.error("Failed to check Twilio Verify for {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to check verification code", e);
        }
    }
}
