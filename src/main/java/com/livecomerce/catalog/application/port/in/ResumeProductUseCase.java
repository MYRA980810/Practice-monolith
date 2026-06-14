package com.livecomerce.catalog.application.port.in;

import java.util.UUID;

public interface ResumeProductUseCase {

    record ResumeCommand(UUID productId, UUID storeId) {}

    void resume(ResumeCommand command);
}
