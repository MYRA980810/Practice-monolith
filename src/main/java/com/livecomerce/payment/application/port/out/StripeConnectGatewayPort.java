package com.livecomerce.payment.application.port.out;

import java.util.List;

public interface StripeConnectGatewayPort {

    record AccountStatus(boolean chargesEnabled, boolean payoutsEnabled, boolean detailsSubmitted) {
    }

    record AccountDetails(
            String businessName,
            String businessUrl,
            String email,
            List<String> currentlyDue,
            List<String> pastDue,
            String disabledReason,
            String bankName,
            String bankLast4,
            String bankAccountStatus
    ) {
    }

    String createExpressAccount(String email);

    String createAccountLink(String stripeAccountId, String refreshUrl, String returnUrl);

    AccountStatus retrieveAccountStatus(String stripeAccountId);

    record BalanceAmount(long amount, String currency) {
    }

    record AccountBalance(List<BalanceAmount> available, List<BalanceAmount> pending) {
    }

    record PayoutItem(
            String id,
            long amount,
            String currency,
            String status,
            String method,
            java.time.OffsetDateTime arrivalDate
    ) {
    }

    record PayoutPage(List<PayoutItem> items, String nextCursor, boolean hasMore) {
    }

    AccountDetails retrieveAccountDetails(String stripeAccountId);

    AccountBalance retrieveBalance(String stripeAccountId);

    PayoutPage listPayouts(String stripeAccountId, String cursor, int limit);
}
