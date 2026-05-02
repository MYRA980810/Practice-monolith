package com.livecomerce.billing.api;

import com.livecomerce.billing.domain.Gateway;
import com.livecomerce.shared.Plan;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(

        @NotNull
        Plan plan,

        @NotNull
        Gateway gateway
) {}
