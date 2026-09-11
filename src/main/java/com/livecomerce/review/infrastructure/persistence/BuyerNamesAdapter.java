package com.livecomerce.review.infrastructure.persistence;

import com.livecomerce.review.application.port.out.LoadBuyerNamesPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class BuyerNamesAdapter implements LoadBuyerNamesPort {

    private final ReviewBuyerNameRepository buyerNameRepository;

    @Override
    public Map<UUID, String> loadNames(Collection<UUID> buyerIds) {
        if (buyerIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> names = new HashMap<>();
        for (Object[] row : buyerNameRepository.findNamesByIds(buyerIds)) {
            var buyerId = (UUID) row[0];
            var fullName = (row[1] + " " + row[2]).trim();
            names.put(buyerId, fullName);
        }
        return names;
    }
}
