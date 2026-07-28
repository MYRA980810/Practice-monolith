package com.livecomerce.live.application.port.in;

import java.util.UUID;

public interface RecordViewerHeartbeatUseCase {

    long recordHeartbeat(RecordHeartbeatCommand command);

    record RecordHeartbeatCommand(UUID liveId, String viewerId) {}
}
