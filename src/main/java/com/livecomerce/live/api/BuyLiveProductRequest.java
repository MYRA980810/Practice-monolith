package com.livecomerce.live.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BuyLiveProductRequest(
        @NotNull UUID liveProductId,
        @Min(1) int quantity
) {}
