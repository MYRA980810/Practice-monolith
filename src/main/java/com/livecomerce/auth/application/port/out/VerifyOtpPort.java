package com.livecomerce.auth.application.port.out;

import com.livecomerce.auth.domain.VerificationChannel;

public interface VerifyOtpPort {
    void send(String to, VerificationChannel channel);
    boolean check(String to, String code, VerificationChannel channel);
}
