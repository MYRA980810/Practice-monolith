package com.livecomerce.order.api;

import jakarta.validation.constraints.NotBlank;

public record ShipOrderRequest(@NotBlank String trackingNumber) {}
