package com.livecomerce.live.application;

import com.livecomerce.live.LiveStartedEvent;
import com.livecomerce.live.application.port.in.StartLiveUseCase;
import com.livecomerce.live.application.port.out.AgoraTokenPort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.LoadLiveSubscriptionPort;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.application.port.out.SaveLiveSubscriptionPort;
import com.livecomerce.live.application.port.out.VideoBroadcastPort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotOwnedBySellerException;
import com.livecomerce.live.domain.VideoProviderUnavailableException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StartLiveService implements StartLiveUseCase {

    private static final Logger log = LoggerFactory.getLogger(StartLiveService.class);

    private static final int TOKEN_TTL_SECONDS = 3600;

    private final LoadLivePort              loadLivePort;
    private final SaveLivePort              saveLivePort;
    private final AgoraTokenPort            agoraTokenPort;
    private final VideoBroadcastPort        videoBroadcastPort;
    private final LoadLiveSubscriptionPort  loadLiveSubscriptionPort;
    private final SaveLiveSubscriptionPort  saveLiveSubscriptionPort;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${live.video-provider:agora}")
    private String videoProvider;

    @Override
    public Live startLive(StartLiveCommand command) {
        var live = loadLivePort.loadById(command.liveId())
                .orElseThrow(() -> new LiveNotFoundException(command.liveId()));

        verifySeller(live, command.sellerId());

        live.start();

        if ("ivs".equals(videoProvider)) {
            try {
                var handle = videoBroadcastPort.createChannel("live-" + live.getId());
                live.setIvsChannel(handle.channelArn(), handle.ingestEndpoint(),
                        handle.streamKeyArn(), handle.streamKeyValue(), handle.playbackUrl());
            } catch (Exception e) {
                log.error("IVS createChannel failed for live {}: {}", live.getId(), e.getMessage(), e);
                throw new VideoProviderUnavailableException("Unable to start video broadcast for live " + live.getId());
            }
        } else {
            var token = agoraTokenPort.generateRtcToken(
                    live.getAgoraChannelId(), command.rtcUid(), TOKEN_TTL_SECONDS);
            live.setStreamToken(token);
        }

        var saved = saveLivePort.save(live);

        var subscriberIds = loadLiveSubscriptionPort.loadSubscriberIdsByLiveId(live.getId());
        eventPublisher.publishEvent(
                new LiveStartedEvent(live.getId(), live.getStoreId(), live.getTitle(), subscriberIds));
        saveLiveSubscriptionPort.deleteAllByLiveId(live.getId());

        return saved;
    }

    private void verifySeller(Live live, java.util.UUID sellerId) {
        if (!live.getSellerId().equals(sellerId)) {
            throw new LiveNotOwnedBySellerException(live.getId(), sellerId);
        }
    }
}
