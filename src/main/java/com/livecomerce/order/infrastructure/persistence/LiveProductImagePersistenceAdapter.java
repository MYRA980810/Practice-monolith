package com.livecomerce.order.infrastructure.persistence;

import com.livecomerce.order.application.port.out.LoadLiveProductImagePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class LiveProductImagePersistenceAdapter implements LoadLiveProductImagePort {

    private final LiveProductImageReadRepository repository;

    @Override
    public Map<UUID, String> findImageUrls(UUID liveId, Collection<UUID> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> result = new HashMap<>();
        for (Object[] row : repository.findImageUrlsByLiveAndProductIds(liveId, productIds)) {
            result.put((UUID) row[0], (String) row[1]);
        }
        return result;
    }
}
