package com.livecomerce.order.api;

import jakarta.validation.constraints.NotBlank;

public record FinalizeOrderRequest(@NotBlank String shippingAddress) {}
