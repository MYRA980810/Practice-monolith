package com.livecomerce.payment.api;

import com.livecomerce.payment.application.port.in.ListSellerPayoutsUseCase;

import java.util.List;

public record PayoutPageResponse(List<PayoutItemResponse> items, String nextCursor, boolean hasMore) {
    public static PayoutPageResponse from(ListSellerPayoutsUseCase.PayoutPageResult result) {
        return new PayoutPageResponse(
                result.items().stream().map(PayoutItemResponse::from).toList(),
                result.nextCursor(),
                result.hasMore()
        );
    }
}
