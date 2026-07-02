package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.CheckLiveSubscriptionUseCase;
import com.livecomerce.live.application.port.out.LoadLiveSubscriptionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckLiveSubscriptionService implements CheckLiveSubscriptionUseCase {

    private final LoadLiveSubscriptionPort loadLiveSubscriptionPort;

    @Override
    public boolean isSubscribed(CheckLiveSubscriptionCommand command) {
        return loadLiveSubscriptionPort.existsByLiveIdAndBuyerId(command.liveId(), command.buyerId());
    }
}
