package com.livecomerce.payment.api;

import com.livecomerce.payment.application.port.in.GetSellerPayoutAccountDetailsUseCase;

import java.util.List;

public record SellerPayoutAccountDetailsResponse(
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
        List<BalanceAmountResponse> balanceAvailable,
        List<BalanceAmountResponse> balancePending
) {
    public static SellerPayoutAccountDetailsResponse from(GetSellerPayoutAccountDetailsUseCase.PayoutAccountDetailsResult result) {
        return new SellerPayoutAccountDetailsResponse(
                result.connected(),
                result.chargesEnabled(),
                result.payoutsEnabled(),
                result.detailsSubmitted(),
                result.businessName(),
                result.businessUrl(),
                result.email(),
                result.currentlyDue(),
                result.pastDue(),
                result.disabledReason(),
                result.bankName(),
                result.bankLast4(),
                result.bankAccountStatus(),
                result.balanceAvailable().stream().map(BalanceAmountResponse::from).toList(),
                result.balancePending().stream().map(BalanceAmountResponse::from).toList()
        );
    }
}
