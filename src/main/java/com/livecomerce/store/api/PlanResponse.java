package com.livecomerce.store.api;

import com.livecomerce.shared.Money;
import com.livecomerce.shared.Plan;

import java.math.BigDecimal;

public record PlanResponse(
        String code,
        String description,
        Money price,
        BigDecimal commissionRate
) {
    public static PlanResponse from(Plan plan) {
        return new PlanResponse(
                plan.getCode(),
                plan.getDescription(),
                plan.getPrice(),
                plan.getCommissionRate()
        );
    }
}
