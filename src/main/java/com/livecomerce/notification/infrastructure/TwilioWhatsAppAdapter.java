package com.livecomerce.notification.infrastructure;

import com.livecomerce.notification.application.port.out.SendWhatsAppOtpPort;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
@RequiredArgsConstructor
class TwilioWhatsAppAdapter implements SendWhatsAppOtpPort {

    private static final Logger log = LoggerFactory.getLogger(TwilioWhatsAppAdapter.class);

    @Value("${twilio.whatsapp-from}")
    private String from;

    @Override
    public void send(String phone, String code) {
        try {
            Message.creator(
                    new PhoneNumber("whatsapp:" + phone),
                    new PhoneNumber(from),
                    "Your verification code is: *" + code + "*\nThis code expires in 10 minutes."
            ).create();
        } catch (Exception e) {
            log.error("Failed to send OTP WhatsApp to {}: {}", phone, e.getMessage());
            throw new RuntimeException("Failed to send verification WhatsApp message", e);
        }
    }
}
