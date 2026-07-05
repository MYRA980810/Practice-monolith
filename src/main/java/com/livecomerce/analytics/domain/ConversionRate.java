package com.livecomerce.analytics.domain;

/**
 * Pure derivation of a conversion rate from units sold vs. units allocated.
 * The single source of truth for BOTH the per-live channels comparison and
 * the frozen live summary snapshot — no independent formula may exist at
 * either call site.
 */
public final class ConversionRate {

    private ConversionRate() {
    }

    public static double compute(long unitsSold, long totalAllocated) {
        return totalAllocated > 0 ? (double) unitsSold / totalAllocated : 0.0;
    }
}
