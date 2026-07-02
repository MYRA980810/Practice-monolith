package com.livecomerce.notification.application.port.in;

import java.util.UUID;

public interface MarkNotificationReadUseCase {

    void markRead(MarkNotificationReadCommand command);

    record MarkNotificationReadCommand(UUID notificationId, UUID userId) {}
}
