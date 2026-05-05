package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.application.port.out.SaveProductPort;
import com.livecomerce.order.OrderItemPaidEvent;
import com.livecomerce.order.OrderItemReleasedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockEventListener {

    private final LoadProductPort loadProductPort;
    private final SaveProductPort saveProductPort;

    @ApplicationModuleListener
    void on(OrderItemPaidEvent event) {
        var product = loadProductPort.loadById(event.productId())
                .orElseThrow(() -> new ProductNotFoundException(event.productId()));
        product.sellStock(event.quantity());
        saveProductPort.save(product);
    }

    @ApplicationModuleListener
    void on(OrderItemReleasedEvent event) {
        var product = loadProductPort.loadById(event.productId())
                .orElseThrow(() -> new ProductNotFoundException(event.productId()));
        product.releaseStock(event.quantity());
        saveProductPort.save(product);
    }
}
