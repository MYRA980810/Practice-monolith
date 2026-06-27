package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.EndLiveUseCase;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotOwnedBySellerException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EndLiveService implements EndLiveUseCase {

    private final LoadLivePort        loadLivePort;
    private final SaveLivePort        saveLivePort;
    private final LiveBroadcastService broadcastService;

    @Override
    public Live endLive(EndLiveCommand command) {
        var live = loadLivePort.loadById(command.liveId())
                .orElseThrow(() -> new LiveNotFoundException(command.liveId()));

        verifySeller(live, command.sellerId());
        live.end();

        var saved = saveLivePort.save(live);
        broadcastService.broadcastLiveEnded(saved.getId());
        return saved;
    }

    private void verifySeller(Live live, UUID sellerId) {
        if (!live.getSellerId().equals(sellerId)) {
            throw new LiveNotOwnedBySellerException(live.getId(), sellerId);
        }
    }
}
