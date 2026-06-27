package com.livecomerce.live.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveBroadcastServiceTest {

    @Mock SimpMessagingTemplate messagingTemplate;
    @InjectMocks LiveBroadcastService sut;

    private static final UUID LIVE_ID         = UUID.randomUUID();
    private static final UUID LIVE_PRODUCT_ID = UUID.randomUUID();

    @Test
    @SuppressWarnings("null") // Mockito ArgumentCaptor.capture() is @Nullable; SimpMessagingTemplate params are @NonNull
    void broadcastProductPinned_sendsToCorrectTopic() {
        sut.broadcastProductPinned(LIVE_ID, LIVE_PRODUCT_ID, "T-Shirt", new BigDecimal("99"), 30);

        var topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSend(topicCaptor.capture(), (Object) any());
        assertThat(topicCaptor.getValue())
                .isEqualTo("/topic/live/" + LIVE_ID + "/product-pinned");
    }

    @Test
    @SuppressWarnings("null")
    void broadcastStockUpdate_sendsToCorrectTopic() {
        sut.broadcastStockUpdate(LIVE_ID, LIVE_PRODUCT_ID, 5);

        var topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSend(topicCaptor.capture(), (Object) any());
        assertThat(topicCaptor.getValue())
                .isEqualTo("/topic/live/" + LIVE_ID + "/stock-update");
    }

    @Test
    @SuppressWarnings("null")
    void broadcastLiveEnded_sendsToCorrectTopic() {
        sut.broadcastLiveEnded(LIVE_ID);

        var topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSend(topicCaptor.capture(), (Object) any());
        assertThat(topicCaptor.getValue())
                .isEqualTo("/topic/live/" + LIVE_ID + "/ended");
    }
}
