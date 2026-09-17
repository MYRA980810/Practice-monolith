package com.livecomerce.notification.application;

import com.livecomerce.cart.CartCheckoutCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class CartCheckoutEventListenerTest {

    @InjectMocks CartCheckoutEventListener sut;

    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();

    @Test
    void onCheckoutSucceeded_logsWithoutThrowing() {
        var event = new CartCheckoutCompletedEvent(
                BUYER_ID, STORE_ID, true, UUID.randomUUID(), new BigDecimal("10.00"), null);

        assertThatCode(() -> sut.on(event)).doesNotThrowAnyException();
    }

    @Test
    void onCheckoutFailed_logsWithoutThrowing() {
        var event = new CartCheckoutCompletedEvent(
                BUYER_ID, STORE_ID, false, null, null, "ALL_ITEMS_UNAVAILABLE");

        assertThatCode(() -> sut.on(event)).doesNotThrowAnyException();
    }
}
