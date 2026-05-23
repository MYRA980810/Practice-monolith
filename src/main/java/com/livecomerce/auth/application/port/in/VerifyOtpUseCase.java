package com.livecomerce.auth.application.port.in;

public interface VerifyOtpUseCase {

    record VerifyCommand(String pendingToken, String code) {}

    AuthResult verify(VerifyCommand command);
}
