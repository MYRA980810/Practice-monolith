package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.StartLiveUseCase;
import com.livecomerce.live.application.port.out.AgoraTokenPort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotOwnedBySellerException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StartLiveService implements StartLiveUseCase {

    private static final int TOKEN_TTL_SECONDS = 3600;

    private final LoadLivePort   loadLivePort;
    private final SaveLivePort   saveLivePort;
    private final AgoraTokenPort agoraTokenPort;

    @Override
    public Live startLive(StartLiveCommand command) {
        var live = loadLivePort.loadById(command.liveId())
                .orElseThrow(() -> new LiveNotFoundException(command.liveId()));

        verifySeller(live, command.sellerId());

        live.start();

        var token = agoraTokenPort.generateRtcToken(
                live.getAgoraChannelId(), command.rtcUid(), TOKEN_TTL_SECONDS);
        live.setStreamToken(token);

        return saveLivePort.save(live);
    }

    private void verifySeller(Live live, java.util.UUID sellerId) {
        if (!live.getSellerId().equals(sellerId)) {
            throw new LiveNotOwnedBySellerException(live.getId(), sellerId);
        }
    }
}
