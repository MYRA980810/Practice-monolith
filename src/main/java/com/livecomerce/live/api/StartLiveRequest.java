package com.livecomerce.live.api;

import jakarta.validation.constraints.NotBlank;

public record StartLiveRequest(@NotBlank String rtcUid) {}
