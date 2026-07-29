package com.livecomerce.payment.application.port.in;

import java.util.UUID;

public interface CreateSetupIntentUseCase {

    record CreateSetupIntentCommand(UUID buyerId, String email) {}

    record SetupIntentResult(String clientSecret) {}

    SetupIntentResult createSetupIntent(CreateSetupIntentCommand cmd);
}
