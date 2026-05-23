package com.livecomerce.auth.api;

import com.livecomerce.auth.application.port.in.PendingVerificationResult;
import com.livecomerce.auth.domain.VerificationChannel;

public record VerificationInitiatedResponse(
        String pendingToken,
        VerificationChannel channel
) {
    public static VerificationInitiatedResponse from(PendingVerificationResult result) {
        return new VerificationInitiatedResponse(result.pendingToken(), result.channel());
    }
}
