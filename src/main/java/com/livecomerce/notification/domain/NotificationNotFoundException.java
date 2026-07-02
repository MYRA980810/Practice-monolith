package com.livecomerce.notification.domain;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(UUID notificationId) {
        super("Notification not found: " + notificationId);
    }
}
