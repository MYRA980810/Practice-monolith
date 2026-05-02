package com.livecomerce.catalog.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

record AddStockRequest(
        @NotNull @Min(1) Integer quantity
) {}
