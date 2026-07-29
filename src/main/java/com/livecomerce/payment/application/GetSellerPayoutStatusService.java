package com.livecomerce.payment.application;

import com.livecomerce.payment.application.port.in.GetSellerPayoutStatusUseCase;
import com.livecomerce.payment.application.port.out.SellerConnectAccountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetSellerPayoutStatusService implements GetSellerPayoutStatusUseCase {

    private final SellerConnectAccountPort sellerConnectAccountPort;

    @Override
    public PayoutStatusResult getStatus(UUID sellerId) {
        return sellerConnectAccountPort.loadByUserId(sellerId)
                .map(account -> new PayoutStatusResult(
                        account.isChargesEnabled(),
                        account.isPayoutsEnabled(),
                        account.isDetailsSubmitted(),
                        true
                ))
                .orElseGet(() -> new PayoutStatusResult(false, false, false, false));
    }
}
