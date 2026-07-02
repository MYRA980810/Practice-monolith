package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.CheckLiveSubscriptionUseCase.CheckLiveSubscriptionCommand;
import com.livecomerce.live.application.port.out.LoadLiveSubscriptionPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckLiveSubscriptionServiceTest {

    @Mock LoadLiveSubscriptionPort loadLiveSubscriptionPort;
    @InjectMocks CheckLiveSubscriptionService sut;

    private static final UUID LIVE_ID  = UUID.randomUUID();
    private static final UUID BUYER_ID = UUID.randomUUID();

    @Test
    void check_whenSubscribed_returnsTrue() {
        when(loadLiveSubscriptionPort.existsByLiveIdAndBuyerId(LIVE_ID, BUYER_ID)).thenReturn(true);

        boolean result = sut.isSubscribed(new CheckLiveSubscriptionCommand(LIVE_ID, BUYER_ID));

        assertThat(result).isTrue();
    }

    @Test
    void check_whenNotSubscribed_returnsFalse() {
        when(loadLiveSubscriptionPort.existsByLiveIdAndBuyerId(LIVE_ID, BUYER_ID)).thenReturn(false);

        boolean result = sut.isSubscribed(new CheckLiveSubscriptionCommand(LIVE_ID, BUYER_ID));

        assertThat(result).isFalse();
    }
}
