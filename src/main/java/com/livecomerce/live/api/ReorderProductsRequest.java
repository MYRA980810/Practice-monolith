package com.livecomerce.live.api;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReorderProductsRequest(@NotNull List<UUID> orderedProductIds) {}
