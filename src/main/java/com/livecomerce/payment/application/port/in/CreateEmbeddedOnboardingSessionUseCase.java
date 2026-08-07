package com.livecomerce.payment.application.port.in;

import java.util.UUID;

public interface CreateEmbeddedOnboardingSessionUseCase {

    record CreateEmbeddedOnboardingSessionCommand(UUID sellerId, String email) {}

    record EmbeddedOnboardingSessionResult(String clientSecret) {}

    EmbeddedOnboardingSessionResult createEmbeddedOnboardingSession(CreateEmbeddedOnboardingSessionCommand cmd);
}
