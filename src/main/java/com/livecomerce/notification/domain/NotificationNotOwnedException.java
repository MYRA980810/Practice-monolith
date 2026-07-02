package com.livecomerce.notification.domain;

import java.util.UUID;

public class NotificationNotOwnedException extends RuntimeException {

    public NotificationNotOwnedException(UUID notificationId) {
        super("Notification " + notificationId + " does not belong to the authenticated user");
    }
}
