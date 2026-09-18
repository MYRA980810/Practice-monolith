package com.livecomerce.notification.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.cart.CartItemDepletedEvent;
import com.livecomerce.notification.application.port.out.SaveNotificationPort;
import com.livecomerce.notification.application.port.out.SendRtmPeerMessagePort;
import com.livecomerce.notification.domain.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartEventListenerTest {

    @Mock SaveNotificationPort   saveNotificationPort;
    @Mock SendRtmPeerMessagePort sendRtmPeerMessagePort;
    @Spy  ObjectMapper           objectMapper = new ObjectMapper();
    @InjectMocks CartEventListener sut;

    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Test
    @SuppressWarnings("null")
    void onCartItemDepleted_savesNotification_andSendsPeerMessageToBuyer() {
        var event = new CartItemDepletedEvent(BUYER_ID, STORE_ID, PRODUCT_ID, null, "Playera", 5, 4);
        when(saveNotificationPort.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        sut.on(event);

        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(saveNotificationPort).save(captor.capture());
        var notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo("cart-item-depleted");
        assertThat(notification.getUserId()).isEqualTo(BUYER_ID);
        assertThat(notification.getLiveId()).isNull();
        assertThat(notification.getPayload()).containsEntry("productId", PRODUCT_ID.toString());

        verify(sendRtmPeerMessagePort).sendPeerMessage(eq(BUYER_ID.toString()), anyString());
    }
}
