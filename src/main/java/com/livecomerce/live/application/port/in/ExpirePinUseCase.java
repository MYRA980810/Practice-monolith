package com.livecomerce.live.application.port.in;

import com.livecomerce.live.domain.LiveProduct;

import java.util.UUID;

public interface ExpirePinUseCase {

    LiveProduct expirePin(ExpirePinCommand command);

    record ExpirePinCommand(UUID liveId, UUID liveProductId, UUID sellerId) {}
}
