package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.GetSellerAddressesUseCase;
import com.livecomerce.store.application.port.out.SellerAddressPort;
import com.livecomerce.store.domain.SellerAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetSellerAddressesService implements GetSellerAddressesUseCase {

    private final SellerAddressPort sellerAddressPort;

    @Override
    public List<SellerAddress> listByUserId(UUID userId) {
        return sellerAddressPort.loadByUserId(userId);
    }
}
