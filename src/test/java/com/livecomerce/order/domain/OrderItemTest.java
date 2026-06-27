package com.livecomerce.order.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemTest {

    private Order openOrder() {
        return Order.open(UUID.randomUUID(), null, null, "MXN");
    }

    @Test
    void addItem_withProductType_storesProductIdVariantIdAndType() {
        var order     = openOrder();
        var productId = UUID.randomUUID();
        var variantId = UUID.randomUUID();

        var item = order.addItem(productId, variantId, "Camisa", new BigDecimal("100"), "MXN", 1, 10, OrderItemType.PRODUCT);

        assertThat(item.getItemType()).isEqualTo(OrderItemType.PRODUCT);
        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(item.getVariantId()).isEqualTo(variantId);
    }

    @Test
    void addItem_withHotProductType_allowsNullProductIdAndVariantId() {
        var order = openOrder();

        var item = order.addItem(null, null, "Producto caliente", new BigDecimal("200"), "MXN", 1, 10, OrderItemType.HOT_PRODUCT);

        assertThat(item.getItemType()).isEqualTo(OrderItemType.HOT_PRODUCT);
        assertThat(item.getProductId()).isNull();
        assertThat(item.getVariantId()).isNull();
    }

    @Test
    void addItem_withShippingType_allowsNullProductIdAndVariantId() {
        var order = openOrder();

        var item = order.addItem(null, null, "Envio express", new BigDecimal("50"), "MXN", 1, 10, OrderItemType.SHIPPING);

        assertThat(item.getItemType()).isEqualTo(OrderItemType.SHIPPING);
        assertThat(item.getProductId()).isNull();
        assertThat(item.getVariantId()).isNull();
    }

    @Test
    void confirmPayment_forProductItem_transitionsToPayment() {
        var order = openOrder();
        var item  = order.addItem(UUID.randomUUID(), UUID.randomUUID(), "Camisa", new BigDecimal("100"), "MXN", 1, 10, OrderItemType.PRODUCT);

        order.confirmItemPayment(item.getId());

        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.PAID);
        assertThat(item.getPaidAt()).isNotNull();
    }

    @Test
    void confirmPayment_forHotProductItem_transitionsToPayment() {
        var order = openOrder();
        var item  = order.addItem(null, null, "Producto caliente", new BigDecimal("200"), "MXN", 1, 10, OrderItemType.HOT_PRODUCT);

        order.confirmItemPayment(item.getId());

        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.PAID);
        assertThat(item.getPaidAt()).isNotNull();
    }

    @Test
    void confirmPayment_forShippingItem_transitionsToPayment() {
        var order = openOrder();
        var item  = order.addItem(null, null, "Envio express", new BigDecimal("50"), "MXN", 1, 10, OrderItemType.SHIPPING);

        order.confirmItemPayment(item.getId());

        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.PAID);
        assertThat(item.getPaidAt()).isNotNull();
    }
}
