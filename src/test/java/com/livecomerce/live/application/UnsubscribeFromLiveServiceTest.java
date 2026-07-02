package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.UnsubscribeFromLiveUseCase.UnsubscribeFromLiveCommand;
import com.livecomerce.live.application.port.out.SaveLiveSubscriptionPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UnsubscribeFromLiveServiceTest {

    @Mock SaveLiveSubscriptionPort saveLiveSubscriptionPort;
    @InjectMocks UnsubscribeFromLiveService sut;

    private static final UUID LIVE_ID  = UUID.randomUUID();
    private static final UUID BUYER_ID = UUID.randomUUID();

    @Test
    void unsubscribe_whenSubscribed_deletesSubscription() {
        sut.unsubscribe(new UnsubscribeFromLiveCommand(LIVE_ID, BUYER_ID));

        verify(saveLiveSubscriptionPort).delete(LIVE_ID, BUYER_ID);
    }

    @Test
    void unsubscribe_whenNotSubscribed_isNoOpViaIdempotentDelete() {
        // The port's delete is idempotent (JPQL DELETE with WHERE clause returns 0 rows without error).
        // The service simply delegates — no prior existence check required.
        sut.unsubscribe(new UnsubscribeFromLiveCommand(LIVE_ID, BUYER_ID));

        verify(saveLiveSubscriptionPort).delete(LIVE_ID, BUYER_ID);
    }
}
