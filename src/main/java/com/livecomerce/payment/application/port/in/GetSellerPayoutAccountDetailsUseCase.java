package com.livecomerce.payment.application.port.in;

import java.util.List;
import java.util.UUID;

public interface GetSellerPayoutAccountDetailsUseCase {

    record BalanceAmountResult(long amount, String currency) {
    }

    record PayoutAccountDetailsResult(
            boolean connected,
            boolean chargesEnabled,
            boolean payoutsEnabled,
            boolean detailsSubmitted,
            String businessName,
            String businessUrl,
            String email,
            List<String> currentlyDue,
            List<String> pastDue,
            String disabledReason,
            String bankName,
            String bankLast4,
            String bankAccountStatus,
            List<BalanceAmountResult> balanceAvailable,
            List<BalanceAmountResult> balancePending
    ) {
        public static PayoutAccountDetailsResult notConnected() {
            return new PayoutAccountDetailsResult(false, false, false, false,
                    null, null, null, List.of(), List.of(), null, null, null, null, List.of(), List.of());
        }
    }

    PayoutAccountDetailsResult getDetails(UUID sellerId);
}
