package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.ReorderProductsUseCase;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.LoadLiveProductPort;
import com.livecomerce.live.application.port.out.SaveLiveProductPort;
import com.livecomerce.live.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReorderProductsService implements ReorderProductsUseCase {

    private final LoadLivePort        loadLivePort;
    private final LoadLiveProductPort loadLiveProductPort;
    private final SaveLiveProductPort saveLiveProductPort;

    @Override
    public List<LiveProduct> reorderProducts(ReorderProductsCommand command) {
        var live = loadLivePort.loadById(command.liveId())
                .orElseThrow(() -> new LiveNotFoundException(command.liveId()));

        verifySeller(live, command.sellerId());

        var existingProducts = loadLiveProductPort.loadByLiveId(command.liveId());
        Map<UUID, LiveProduct> byId = existingProducts.stream()
                .collect(Collectors.toMap(LiveProduct::getId, Function.identity()));

        var orderedIds = command.orderedProductIds();
        for (UUID id : orderedIds) {
            if (!byId.containsKey(id)) {
                throw new IllegalArgumentException("LiveProduct not found in this live: " + id);
            }
        }

        for (int i = 0; i < orderedIds.size(); i++) {
            byId.get(orderedIds.get(i)).setPosition(i);
        }

        return saveLiveProductPort.saveAll(
                orderedIds.stream().map(byId::get).collect(Collectors.toList()));
    }

    private void verifySeller(Live live, UUID sellerId) {
        if (!live.getSellerId().equals(sellerId)) {
            throw new LiveNotOwnedBySellerException(live.getId(), sellerId);
        }
    }
}
