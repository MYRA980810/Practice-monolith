package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.out.LoadProductVariantPort;
import com.livecomerce.catalog.application.port.out.SaveProductVariantPort;
import com.livecomerce.order.OrderItemPaidEvent;
import com.livecomerce.order.OrderItemReleasedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockEventListener {

    private final LoadProductVariantPort loadProductVariantPort;
    private final SaveProductVariantPort saveProductVariantPort;

    @ApplicationModuleListener
    void on(OrderItemPaidEvent event) {
        if (event.variantId() == null) return;
        var variant = loadProductVariantPort.loadById(event.variantId())
                .orElseThrow(() -> new ProductVariantNotFoundException(event.variantId()));
        variant.sellStock(event.quantity());
        saveProductVariantPort.save(variant);
    }

    @ApplicationModuleListener
    void on(OrderItemReleasedEvent event) {
        if (event.variantId() == null) return;
        var variant = loadProductVariantPort.loadById(event.variantId())
                .orElseThrow(() -> new ProductVariantNotFoundException(event.variantId()));
        variant.releaseStock(event.quantity());
        saveProductVariantPort.save(variant);
    }
}
