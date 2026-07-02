package com.livecomerce.notification.application.port.out;

import com.livecomerce.notification.domain.Notification;

public interface SaveNotificationPort {

    Notification save(Notification notification);
}
