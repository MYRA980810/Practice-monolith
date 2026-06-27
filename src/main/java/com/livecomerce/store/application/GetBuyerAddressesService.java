package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.GetBuyerAddressesUseCase;
import com.livecomerce.store.application.port.out.BuyerAddressPort;
import com.livecomerce.store.domain.BuyerAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetBuyerAddressesService implements GetBuyerAddressesUseCase {

    private final BuyerAddressPort buyerAddressPort;

    @Override
    public List<BuyerAddress> listByUserId(UUID userId) {
        return buyerAddressPort.loadByUserId(userId);
    }
}
