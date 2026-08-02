package com.livecomerce.payment.application;

import com.livecomerce.payment.SellerPayoutAccountActivatedEvent;
import com.livecomerce.payment.application.port.in.GetSellerPayoutStatusUseCase;
import com.livecomerce.payment.application.port.out.SellerConnectAccountPort;
import com.livecomerce.payment.application.port.out.StripeConnectGatewayPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class GetSellerPayoutStatusService implements GetSellerPayoutStatusUseCase {

    private final SellerConnectAccountPort sellerConnectAccountPort;
    private final StripeConnectGatewayPort stripeConnectGatewayPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PayoutStatusResult getStatus(UUID sellerId) {
        var account = sellerConnectAccountPort.loadByUserId(sellerId).orElse(null);
        if (account == null) {
            return new PayoutStatusResult(false, false, false, false);
        }

        // Cache is normally kept fresh by the account.updated webhook, but that delivery
        // can be delayed or missed locally — reconcile against Stripe directly whenever
        // the cached row isn't fully active yet, so status never gets stuck stale.
        if (!(account.isChargesEnabled() && account.isPayoutsEnabled() && account.isDetailsSubmitted())) {
            boolean wasActive = account.getActivatedAt() != null;

            var live = stripeConnectGatewayPort.retrieveAccountStatus(account.getStripeAccountId());
            account.updateCapabilities(live.chargesEnabled(), live.payoutsEnabled(), live.detailsSubmitted());
            account = sellerConnectAccountPort.save(account);

            if (!wasActive && account.getActivatedAt() != null) {
                eventPublisher.publishEvent(new SellerPayoutAccountActivatedEvent(account.getUserId(), account.getId()));
            }
        }

        return new PayoutStatusResult(
                account.isChargesEnabled(),
                account.isPayoutsEnabled(),
                account.isDetailsSubmitted(),
                true
        );
    }
}
