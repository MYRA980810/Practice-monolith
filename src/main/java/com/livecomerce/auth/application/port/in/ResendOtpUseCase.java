package com.livecomerce.auth.application.port.in;

public interface ResendOtpUseCase {

    record ResendCommand(String pendingToken) {}

    void resend(ResendCommand command);
}
