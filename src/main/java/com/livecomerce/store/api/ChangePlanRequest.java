package com.livecomerce.store.api;

import com.livecomerce.shared.Plan;
import jakarta.validation.constraints.NotNull;

public record ChangePlanRequest(

        @NotNull
        Plan plan
) {}
