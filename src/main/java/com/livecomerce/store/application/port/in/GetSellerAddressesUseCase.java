package com.livecomerce.store.application.port.in;

import com.livecomerce.store.domain.SellerAddress;

import java.util.List;
import java.util.UUID;

public interface GetSellerAddressesUseCase {

    List<SellerAddress> listByUserId(UUID userId);
}
