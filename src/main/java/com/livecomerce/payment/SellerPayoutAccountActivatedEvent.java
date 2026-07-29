package com.livecomerce.payment;

import java.util.UUID;

public record SellerPayoutAccountActivatedEvent(UUID userId, UUID sellerConnectAccountId) {}
