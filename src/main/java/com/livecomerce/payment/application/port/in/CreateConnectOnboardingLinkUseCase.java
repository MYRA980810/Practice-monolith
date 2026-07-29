package com.livecomerce.payment.application.port.in;

import java.util.UUID;

public interface CreateConnectOnboardingLinkUseCase {

    record CreateOnboardingLinkCommand(UUID sellerId, String email) {}

    record OnboardingLinkResult(String url) {}

    OnboardingLinkResult createOnboardingLink(CreateOnboardingLinkCommand cmd);
}
