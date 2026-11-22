package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.ExpirePinUseCase;
import com.livecomerce.live.application.port.out.LiveRoomEventPort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.LoadLiveProductPort;
import com.livecomerce.live.application.port.out.RoomEvent;
import com.livecomerce.live.application.port.out.SaveLiveProductPort;
import com.livecomerce.live.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpirePinService implements ExpirePinUseCase {

    private final LoadLivePort        loadLivePort;
    private final LoadLiveProductPort loadLiveProductPort;
    private final SaveLiveProductPort saveLiveProductPort;
    private final LiveRoomEventPort   liveRoomEventPort;

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

        liveRoomEventPort.publish(live, new RoomEvent.ProductExpired(saved.getId(), saved.getStatus().name()));

        return saved;
    }

    private void verifySeller(Live live, java.util.UUID sellerId) {
        if (!live.getSellerId().equals(sellerId)) {
            throw new LiveNotOwnedBySellerException(live.getId(), sellerId);
        }
    }
}
