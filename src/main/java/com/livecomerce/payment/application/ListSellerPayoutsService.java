package com.livecomerce.payment.application;

import com.livecomerce.payment.application.port.in.ListSellerPayoutsUseCase;
import com.livecomerce.payment.application.port.out.SellerConnectAccountPort;
import com.livecomerce.payment.application.port.out.StripeConnectGatewayPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListSellerPayoutsService implements ListSellerPayoutsUseCase {

    private final SellerConnectAccountPort sellerConnectAccountPort;
    private final StripeConnectGatewayPort stripeConnectGatewayPort;

    @Override
    public PayoutPageResult listPayouts(UUID sellerId, String cursor, int limit) {
        var account = sellerConnectAccountPort.loadByUserId(sellerId).orElse(null);
        if (account == null) {
            return PayoutPageResult.empty();
        }

        var page = stripeConnectGatewayPort.listPayouts(account.getStripeAccountId(), cursor, limit);

        var items = page.items().stream()
                .map(item -> new PayoutItemResult(
                        item.id(),
                        item.amount(),
                        item.currency(),
                        item.status(),
                        item.method(),
                        item.arrivalDate()))
                .toList();

        return new PayoutPageResult(items, page.nextCursor(), page.hasMore());
    }
}
