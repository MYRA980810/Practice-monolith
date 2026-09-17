package com.livecomerce.notification.application;

import com.livecomerce.cart.CartCheckoutCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Handles {@link CartCheckoutCompletedEvent} (R4-002 fix): a first
 * observability hook for checkout outcomes, purely a structured, greppable
 * log line — no buyer-facing notification exists for this event yet, so
 * this listener does not invent one.
 */
@Component
@RequiredArgsConstructor
public class CartCheckoutEventListener {

    private static final Logger log = LoggerFactory.getLogger(CartCheckoutEventListener.class);

    @ApplicationModuleListener
    public void on(CartCheckoutCompletedEvent event) {
        if (event.succeeded()) {
            log.info("Checkout succeeded for buyer {} store {}: orderId {}",
                    event.buyerId(), event.storeId(), event.orderId());
        } else {
            log.warn("Checkout failed for buyer {} store {}: {}",
                    event.buyerId(), event.storeId(), event.failureReason());
        }
    }
}
