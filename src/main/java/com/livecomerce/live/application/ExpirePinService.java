package com.livecomerce.live.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.port.in.ExpirePinUseCase;
import com.livecomerce.live.application.port.out.AgoraRtmMessagePort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.LoadLiveProductPort;
import com.livecomerce.live.application.port.out.SaveLiveProductPort;
import com.livecomerce.live.domain.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpirePinService implements ExpirePinUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExpirePinService.class);

    private final LoadLivePort        loadLivePort;
    private final LoadLiveProductPort loadLiveProductPort;
    private final SaveLiveProductPort saveLiveProductPort;
    private final AgoraRtmMessagePort agoraRtmMessagePort;
    private final ObjectMapper        objectMapper;

    @Override
    public LiveProduct expirePin(ExpirePinCommand command) {
        var live = loadLivePort.loadById(command.liveId())
                .orElseThrow(() -> new LiveNotFoundException(command.liveId()));

        verifySeller(live, command.sellerId());

        var lp = loadLiveProductPort.loadById(command.liveProductId())
                .orElseThrow(() -> new LiveProductNotFoundException(command.liveProductId()));

        if (lp.getStatus() != LiveProductStatus.PINNED) {
            return lp;
        }

        if (lp.isStockExhausted()) {
            lp.markAsSold();
        } else {
            lp.unpin();
        }

        var saved = saveLiveProductPort.save(lp);

        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "type",          "product-expired",
                    "liveProductId", saved.getId(),
                    "status",        saved.getStatus().name()
            ));
            agoraRtmMessagePort.sendChannelMessage("live-chat:" + live.getId(), payload);
        } catch (Exception e) {
            log.warn("Agora RTM product-expired failed: {}", e.getMessage());
        }

        return saved;
    }

    private void verifySeller(Live live, java.util.UUID sellerId) {
        if (!live.getSellerId().equals(sellerId)) {
            throw new LiveNotOwnedBySellerException(live.getId(), sellerId);
        }
    }
}
