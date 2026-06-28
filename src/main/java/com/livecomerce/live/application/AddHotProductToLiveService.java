package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.AddHotProductUseCase;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveLiveProductPort;
import com.livecomerce.live.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AddHotProductToLiveService implements AddHotProductUseCase {

    private final LoadLivePort        loadLivePort;
    private final SaveLiveProductPort saveLiveProductPort;

    @Override
    public LiveProduct addHotProduct(AddHotProductCommand command) {
        var live = loadLivePort.loadById(command.liveId())
                .orElseThrow(() -> new LiveNotFoundException(command.liveId()));

        verifySeller(live, command.sellerId());
        verifyLiveActiveForProducts(live);

        var lp = LiveProduct.forHotProduct(
                live, command.name(), command.price(), command.currency(),
                command.stockAllocated(), command.imageUrl());

        return saveLiveProductPort.save(lp);
    }

    private void verifySeller(Live live, UUID sellerId) {
        if (!live.getSellerId().equals(sellerId)) {
            throw new LiveNotOwnedBySellerException(live.getId(), sellerId);
        }
    }

    private void verifyLiveActiveForProducts(Live live) {
        if (live.getStatus() != LiveStatus.SCHEDULED && live.getStatus() != LiveStatus.LIVE) {
            throw new IllegalStateException(
                    "Cannot add products to live in status: " + live.getStatus());
        }
    }
}
