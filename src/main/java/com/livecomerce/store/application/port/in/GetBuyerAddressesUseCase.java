package com.livecomerce.store.application.port.in;

import com.livecomerce.store.domain.BuyerAddress;

import java.util.List;
import java.util.UUID;

public interface GetBuyerAddressesUseCase {

    List<BuyerAddress> listByUserId(UUID userId);
}
