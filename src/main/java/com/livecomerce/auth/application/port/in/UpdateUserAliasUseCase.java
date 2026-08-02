package com.livecomerce.auth.application.port.in;

import java.util.UUID;

public interface UpdateUserAliasUseCase {

    String updateAlias(UpdateUserAliasCommand command);

    record UpdateUserAliasCommand(UUID userId, String alias) {}
}
