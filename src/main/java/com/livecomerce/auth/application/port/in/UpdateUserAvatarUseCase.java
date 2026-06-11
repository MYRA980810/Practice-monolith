package com.livecomerce.auth.application.port.in;

import java.util.UUID;

public interface UpdateUserAvatarUseCase {

    String updateAvatar(UpdateUserAvatarCommand command);

    record UpdateUserAvatarCommand(UUID userId, String avatarUrl) {}
}
