package com.livecomerce.store.application.port.out;

import com.livecomerce.store.domain.SellerAddress;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SellerAddressPort {

    SellerAddress save(SellerAddress address);

    Optional<SellerAddress> loadById(UUID id);

    List<SellerAddress> loadByUserId(UUID userId);

    Optional<SellerAddress> loadDefaultByUserId(UUID userId);

    void clearDefaultForUser(UUID userId);

    void deleteById(UUID id);

    int countByUserId(UUID userId);
}
