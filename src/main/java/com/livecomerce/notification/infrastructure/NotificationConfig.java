package com.livecomerce.notification.infrastructure;

import com.resend.Resend;
import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!local")
class NotificationConfig {

    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${twilio.account-sid}")
    private String twilioAccountSid;

    @Value("${twilio.auth-token}")
    private String twilioAuthToken;

    @Bean
    Resend resend() {
        return new Resend(resendApiKey);
    }

    @PostConstruct
    void initTwilio() {
        Twilio.init(twilioAccountSid, twilioAuthToken);
    }
}
