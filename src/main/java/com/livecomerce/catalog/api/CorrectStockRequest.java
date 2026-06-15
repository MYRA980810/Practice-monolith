package com.livecomerce.catalog.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

record CorrectStockRequest(
        @NotNull @Min(0) Integer availableQuantity
) {}
