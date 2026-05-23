package com.livecomerce.notification.infrastructure;

import com.livecomerce.notification.application.port.out.SendEmailOtpPort;
import com.livecomerce.notification.application.port.out.SendWhatsAppOtpPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
class LoggingOtpAdapter implements SendEmailOtpPort, SendWhatsAppOtpPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpAdapter.class);

    @Override
    public void send(String recipient, String code) {
        log.info("[LOCAL] OTP → {} | code: {}", recipient, code);
    }
}
