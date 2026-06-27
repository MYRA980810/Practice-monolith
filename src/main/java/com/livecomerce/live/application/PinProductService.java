package com.livecomerce.live.application;

import com.livecomerce.live.LiveProductPinnedEvent;
import com.livecomerce.live.application.port.in.PinProductUseCase;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.LoadLiveProductPort;
import com.livecomerce.live.application.port.out.SaveLiveProductPort;
import com.livecomerce.live.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PinProductService implements PinProductUseCase {

    private final LoadLivePort              loadLivePort;
    private final LoadLiveProductPort       loadLiveProductPort;
    private final SaveLiveProductPort       saveLiveProductPort;
    private final ApplicationEventPublisher eventPublisher;
    private final LiveBroadcastService      broadcastService;

    @Override
    public LiveProduct pinProduct(PinProductCommand command) {
        var live = loadLivePort.loadById(command.liveId())
                .orElseThrow(() -> new LiveNotFoundException(command.liveId()));

        verifySeller(live, command.sellerId());

        var toPin = loadLiveProductPort.loadById(command.liveProductId())
                .orElseThrow(() -> new LiveProductNotFoundException(command.liveProductId()));

        // Unpin the previous pinned product, if any
        loadLiveProductPort.loadPinnedByLiveId(command.liveId())
                .ifPresent(previous -> {
                    previous.unpin();
                    saveLiveProductPort.save(previous);
                });

        toPin.pin();
        var saved = saveLiveProductPort.save(toPin);

        eventPublisher.publishEvent(new LiveProductPinnedEvent(
                live.getId(), saved.getId(),
                saved.getProductNameSnapshot(), saved.getPriceSnapshot()));

        broadcastService.broadcastProductPinned(
                live.getId(), saved.getId(),
                saved.getProductNameSnapshot(), saved.getPriceSnapshot(),
                live.getDisplayDurationSeconds());

        return saved;
    }

    private void verifySeller(Live live, UUID sellerId) {
        if (!live.getSellerId().equals(sellerId)) {
            throw new LiveNotOwnedBySellerException(live.getId(), sellerId);
        }
    }
}
