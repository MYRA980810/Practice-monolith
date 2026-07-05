package com.livecomerce.analytics.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConversionRateTest {

    @Test
    void compute_normalRatio_returnsUnitsSoldOverTotalAllocated() {
        assertThat(ConversionRate.compute(30, 100)).isEqualTo(0.30);
    }

    @Test
    void compute_totalAllocatedZero_returnsZeroWithoutThrowing() {
        assertThat(ConversionRate.compute(5, 0)).isEqualTo(0.0);
    }

    @Test
    void compute_totalAllocatedNegative_returnsZero() {
        assertThat(ConversionRate.compute(5, -1)).isEqualTo(0.0);
    }

    @Test
    void compute_unitsSoldZero_returnsZero() {
        assertThat(ConversionRate.compute(0, 50)).isEqualTo(0.0);
    }
}
