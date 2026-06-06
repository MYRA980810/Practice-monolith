package com.livecomerce.auth.application.port.out;

import java.util.UUID;

public record OAuthCodePayload(UUID userId, OAuthTokenType tokenType) {
}
