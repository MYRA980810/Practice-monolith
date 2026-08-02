package com.livecomerce.payment.application;

import com.livecomerce.payment.application.port.in.GetSellerPayoutAccountDetailsUseCase;
import com.livecomerce.payment.application.port.out.SellerConnectAccountPort;
import com.livecomerce.payment.application.port.out.StripeConnectGatewayPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetSellerPayoutAccountDetailsService implements GetSellerPayoutAccountDetailsUseCase {

    private final SellerConnectAccountPort sellerConnectAccountPort;
    private final StripeConnectGatewayPort stripeConnectGatewayPort;

    @Override
    public PayoutAccountDetailsResult getDetails(UUID sellerId) {
        var account = sellerConnectAccountPort.loadByUserId(sellerId).orElse(null);
        if (account == null) {
            return PayoutAccountDetailsResult.notConnected();
        }

        var details = stripeConnectGatewayPort.retrieveAccountDetails(account.getStripeAccountId());
        var balance = stripeConnectGatewayPort.retrieveBalance(account.getStripeAccountId());

        return new PayoutAccountDetailsResult(
                true,
                account.isChargesEnabled(),
                account.isPayoutsEnabled(),
                account.isDetailsSubmitted(),
                details.businessName(),
                details.businessUrl(),
                details.email(),
                details.currentlyDue(),
                details.pastDue(),
                details.disabledReason(),
                details.bankName(),
                details.bankLast4(),
                details.bankAccountStatus(),
                toBalanceAmountResults(balance.available()),
                toBalanceAmountResults(balance.pending())
        );
    }

    private List<BalanceAmountResult> toBalanceAmountResults(List<StripeConnectGatewayPort.BalanceAmount> amounts) {
        return amounts.stream()
                .map(amount -> new BalanceAmountResult(amount.amount(), amount.currency()))
                .toList();
    }
}
