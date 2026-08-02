package com.livecomerce.auth.application.port.in;

import java.util.UUID;

public interface GetCurrentUserUseCase {

    UserProfileResult getCurrentUser(UUID userId);
}
