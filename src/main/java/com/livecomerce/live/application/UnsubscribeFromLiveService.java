package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.UnsubscribeFromLiveUseCase;
import com.livecomerce.live.application.port.out.SaveLiveSubscriptionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UnsubscribeFromLiveService implements UnsubscribeFromLiveUseCase {

    private final SaveLiveSubscriptionPort saveLiveSubscriptionPort;

    @Override
    public void unsubscribe(UnsubscribeFromLiveCommand command) {
        // Idempotent: the JPQL DELETE is a no-op when no row exists, so no
        // existence check is needed before delegating to the port.
        saveLiveSubscriptionPort.delete(command.liveId(), command.buyerId());
    }
}
