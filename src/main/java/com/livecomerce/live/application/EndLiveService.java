package com.livecomerce.live.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.LiveEndedEvent;
import com.livecomerce.live.application.port.in.EndLiveUseCase;
import com.livecomerce.live.application.port.out.AgoraRtmMessagePort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotOwnedBySellerException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EndLiveService implements EndLiveUseCase {

    private static final Logger log = LoggerFactory.getLogger(EndLiveService.class);

    private final LoadLivePort              loadLivePort;
    private final SaveLivePort              saveLivePort;
    private final AgoraRtmMessagePort       agoraRtmMessagePort;
    private final ObjectMapper              objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Live endLive(EndLiveCommand command) {
        var live = loadLivePort.loadById(command.liveId())
                .orElseThrow(() -> new LiveNotFoundException(command.liveId()));

        verifySeller(live, command.sellerId());
        live.end();

        var saved = saveLivePort.save(live);

        eventPublisher.publishEvent(new LiveEndedEvent(saved.getId(), saved.getSellerId()));

        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "type",   "live-ended",
                    "liveId", saved.getId()
            ));
            agoraRtmMessagePort.sendChannelMessage("live-chat:" + saved.getId(), payload);
        } catch (Exception e) {
            log.warn("Agora RTM live-ended failed: {}", e.getMessage());
        }

        return saved;
    }

    private void verifySeller(Live live, UUID sellerId) {
        if (!live.getSellerId().equals(sellerId)) {
            throw new LiveNotOwnedBySellerException(live.getId(), sellerId);
        }
    }
}
