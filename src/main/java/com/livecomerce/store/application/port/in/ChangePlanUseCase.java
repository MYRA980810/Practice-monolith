package com.livecomerce.store.application.port.in;

import com.livecomerce.shared.Plan;
import com.livecomerce.store.domain.Store;

import java.util.UUID;

public interface ChangePlanUseCase {

    record ChangePlanCommand(
            UUID userId,
            Plan plan
    ) {}

    Store change(ChangePlanCommand command);
}
