package com.livecomerce.auth.infrastructure;

import com.livecomerce.auth.application.port.out.VerifyOtpPort;
import com.livecomerce.auth.domain.VerificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
class LoggingVerifyAdapter implements VerifyOtpPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingVerifyAdapter.class);

    @Override
    public void send(String to, VerificationChannel channel) {
        log.info("[LOCAL] Verify → {} | channel: {}", to, channel);
    }

    @Override
    public boolean check(String to, String code, VerificationChannel channel) {
        log.info("[LOCAL] Check → {} | channel: {} | code: {} → approved", to, channel, code);
        return true;
    }
}
