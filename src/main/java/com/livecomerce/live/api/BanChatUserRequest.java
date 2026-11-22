package com.livecomerce.live.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** {@code reason} is capped at 256 characters, the IVS Chat DisconnectUser limit. */
public record BanChatUserRequest(@NotNull UUID userId, @Size(max = 256) String reason) {}
