package com.livecomerce.live.api;

import jakarta.annotation.Nullable;

public record ExitLiveRequest(
        @Nullable String shippingAddress,
        @Nullable String stripePaymentMethodId
) {}
