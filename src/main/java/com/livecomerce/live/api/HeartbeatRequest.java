package com.livecomerce.live.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HeartbeatRequest(@NotBlank @Size(max = 128) String viewerId) {}
