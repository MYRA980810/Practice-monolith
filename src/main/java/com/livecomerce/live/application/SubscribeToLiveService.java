package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.SubscribeToLiveUseCase;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.LoadLiveSubscriptionPort;
import com.livecomerce.live.application.port.out.SaveLiveSubscriptionPort;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotScheduledException;
import com.livecomerce.live.domain.LiveStatus;
import com.livecomerce.live.domain.LiveSubscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscribeToLiveService implements SubscribeToLiveUseCase {

    private final LoadLivePort             loadLivePort;
    private final LoadLiveSubscriptionPort loadLiveSubscriptionPort;
    private final SaveLiveSubscriptionPort saveLiveSubscriptionPort;

    @Override
    public void subscribe(SubscribeToLiveCommand command) {
        var live = loadLivePort.loadById(command.liveId())
                .orElseThrow(() -> new LiveNotFoundException(command.liveId()));

        if (live.getStatus() != LiveStatus.SCHEDULED) {
            throw new LiveNotScheduledException(command.liveId());
        }

        if (loadLiveSubscriptionPort.existsByLiveIdAndBuyerId(command.liveId(), command.buyerId())) {
            return; // idempotent — already subscribed
        }

        saveLiveSubscriptionPort.save(LiveSubscription.create(command.liveId(), command.buyerId()));
    }
}
