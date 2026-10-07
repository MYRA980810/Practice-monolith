package com.livecomerce.live.application;

import com.livecomerce.live.LiveCancelledEvent;
import com.livecomerce.live.application.port.in.CancelLiveUseCase;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.LoadLiveSubscriptionPort;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.application.port.out.SaveLiveSubscriptionPort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotOwnedBySellerException;
import com.livecomerce.live.domain.LiveStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CancelLiveService implements CancelLiveUseCase {

    private final LoadLivePort              loadLivePort;
    private final SaveLivePort              saveLivePort;
    private final LoadLiveSubscriptionPort  loadLiveSubscriptionPort;
    private final SaveLiveSubscriptionPort  saveLiveSubscriptionPort;
    private final LiveRoomCloser            liveRoomCloser;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Live cancelLive(CancelLiveCommand command) {
        var live = loadLivePort.loadById(command.liveId())
                .orElseThrow(() -> new LiveNotFoundException(command.liveId()));

        verifySeller(live, command.sellerId());
        boolean wasLive = live.getStatus() == LiveStatus.LIVE
                || live.getStatus() == LiveStatus.RECONNECTING;
        live.cancel();

        var saved = saveLivePort.save(live);

        var subscriberIds = loadLiveSubscriptionPort.loadSubscriberIdsByLiveId(live.getId());
        eventPublisher.publishEvent(new LiveCancelledEvent(
                saved.getId(), saved.getSellerId(), saved.getStoreId(), saved.getTitle(),
                subscriberIds, wasLive, saved.getUpdatedAt().toInstant()));
        saveLiveSubscriptionPort.deleteAllByLiveId(live.getId());

        if (wasLive) {
            liveRoomCloser.closeRoom(live);
        }

        return saved;
    }

    private void verifySeller(Live live, UUID sellerId) {
        if (!live.getSellerId().equals(sellerId)) {
            throw new LiveNotOwnedBySellerException(live.getId(), sellerId);
        }
    }
}
