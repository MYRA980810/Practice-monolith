package com.livecomerce.auth.application.port.in;

import com.livecomerce.auth.domain.VerificationChannel;

public record PendingVerificationResult(
        String pendingToken,
        VerificationChannel channel
) {}
