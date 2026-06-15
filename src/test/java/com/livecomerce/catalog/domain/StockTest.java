package com.livecomerce.catalog.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    private Stock stock;

    @BeforeEach
    void setUp() {
        var product = Product.create(UUID.randomUUID(), "Producto", null, BigDecimal.TEN, "MXN", null, null);
        stock = product.defaultVariant().getStock();
    }

    // --- add ---

    @Test
    void add_withPositiveQty_increasesTotalAndAvailable() {
        stock.add(10);

        assertThat(stock.getTotalQuantity()).isEqualTo(10);
        assertThat(stock.getAvailableQuantity()).isEqualTo(10);
        assertThat(stock.getReservedQuantity()).isEqualTo(0);
    }

    @Test
    void add_withNonPositiveQty_throwsIllegalArgument() {
        assertThatThrownBy(() -> stock.add(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stock.add(-5)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- canReserve ---

    @Test
    void canReserve_whenSufficientStock_returnsTrue() {
        stock.add(5);
        assertThat(stock.canReserve(5)).isTrue();
        assertThat(stock.canReserve(3)).isTrue();
    }

    @Test
    void canReserve_whenInsufficientStock_returnsFalse() {
        stock.add(3);
        assertThat(stock.canReserve(4)).isFalse();
    }

    @Test
    void canReserve_whenEmptyStock_returnsFalse() {
        assertThat(stock.canReserve(1)).isFalse();
    }

    // --- reserve ---

    @Test
    void reserve_withSufficientStock_movesFromAvailableToReserved() {
        stock.add(10);
        stock.reserve(3);

        assertThat(stock.getTotalQuantity()).isEqualTo(10);
        assertThat(stock.getAvailableQuantity()).isEqualTo(7);
        assertThat(stock.getReservedQuantity()).isEqualTo(3);
    }

    @Test
    void reserve_withInsufficientStock_throwsIllegalState() {
        stock.add(2);

        assertThatThrownBy(() -> stock.reserve(5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void reserve_withNonPositiveQty_throwsIllegalArgument() {
        assertThatThrownBy(() -> stock.reserve(0)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- release ---

    @Test
    void release_movesFromReservedBackToAvailable() {
        stock.add(10);
        stock.reserve(4);
        stock.release(2);

        assertThat(stock.getAvailableQuantity()).isEqualTo(8);
        assertThat(stock.getReservedQuantity()).isEqualTo(2);
        assertThat(stock.getTotalQuantity()).isEqualTo(10);
    }

    @Test
    void release_withNonPositiveQty_throwsIllegalArgument() {
        assertThatThrownBy(() -> stock.release(0)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- sell ---

    @Test
    void sell_decreasesReservedAndTotal() {
        stock.add(10);
        stock.reserve(3);
        stock.sell(3);

        assertThat(stock.getTotalQuantity()).isEqualTo(7);
        assertThat(stock.getReservedQuantity()).isEqualTo(0);
        assertThat(stock.getAvailableQuantity()).isEqualTo(7);
    }

    @Test
    void sell_withNonPositiveQty_throwsIllegalArgument() {
        assertThatThrownBy(() -> stock.sell(0)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- correctAvailable ---

    @Test
    void correctAvailable_toHigherValue_setsAvailableAndRecomputesTotal() {
        stock.add(10);
        stock.correctAvailable(20);

        assertThat(stock.getAvailableQuantity()).isEqualTo(20);
        assertThat(stock.getTotalQuantity()).isEqualTo(20);
        assertThat(stock.getReservedQuantity()).isEqualTo(0);
    }

    @Test
    void correctAvailable_toLowerValue_setsAvailableAndRecomputesTotal() {
        stock.add(20);
        stock.correctAvailable(8);

        assertThat(stock.getAvailableQuantity()).isEqualTo(8);
        assertThat(stock.getTotalQuantity()).isEqualTo(8);
        assertThat(stock.getReservedQuantity()).isEqualTo(0);
    }

    @Test
    void correctAvailable_toZero_setsAvailableToZeroAndTotalToReserved() {
        stock.add(10);
        stock.correctAvailable(0);

        assertThat(stock.getAvailableQuantity()).isEqualTo(0);
        assertThat(stock.getTotalQuantity()).isEqualTo(0);
    }

    @Test
    void correctAvailable_withReserved_recomputesTotalAndLeavesReservedUntouched() {
        stock.add(20);
        stock.reserve(10);
        stock.correctAvailable(8);

        assertThat(stock.getAvailableQuantity()).isEqualTo(8);
        assertThat(stock.getReservedQuantity()).isEqualTo(10);
        assertThat(stock.getTotalQuantity()).isEqualTo(18);
    }

    @Test
    void correctAvailable_withNegativeValue_throwsIllegalArgument() {
        assertThatThrownBy(() -> stock.correctAvailable(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }
}
