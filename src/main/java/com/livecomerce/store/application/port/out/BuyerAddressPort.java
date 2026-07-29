package com.livecomerce.store.application.port.out;

import com.livecomerce.store.domain.BuyerAddress;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BuyerAddressPort {

    BuyerAddress save(BuyerAddress address);

    Optional<BuyerAddress> loadById(UUID id);

    List<BuyerAddress> loadByUserId(UUID userId);

    Optional<BuyerAddress> loadDefaultByUserId(UUID userId);

    void clearDefaultForUser(UUID userId);

    void deleteById(UUID id);

    int countByUserId(UUID userId);
}
