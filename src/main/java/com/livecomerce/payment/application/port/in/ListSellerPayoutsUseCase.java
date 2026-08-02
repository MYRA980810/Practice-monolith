package com.livecomerce.payment.application.port.in;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ListSellerPayoutsUseCase {

    record PayoutItemResult(String id, long amount, String currency, String status, String method, OffsetDateTime arrivalDate) {
    }

    record PayoutPageResult(List<PayoutItemResult> items, String nextCursor, boolean hasMore) {
        public static PayoutPageResult empty() {
            return new PayoutPageResult(List.of(), null, false);
        }
    }

    PayoutPageResult listPayouts(UUID sellerId, String cursor, int limit);
}
