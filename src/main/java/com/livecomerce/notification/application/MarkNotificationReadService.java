package com.livecomerce.notification.application;

import com.livecomerce.notification.domain.NotificationNotFoundException;
import com.livecomerce.notification.domain.NotificationNotOwnedException;
import com.livecomerce.notification.application.port.in.MarkNotificationReadUseCase;
import com.livecomerce.notification.application.port.out.LoadNotificationPort;
import com.livecomerce.notification.application.port.out.SaveNotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MarkNotificationReadService implements MarkNotificationReadUseCase {

    private final LoadNotificationPort loadNotificationPort;
    private final SaveNotificationPort saveNotificationPort;

    @Override
    public void markRead(MarkNotificationReadCommand command) {
        var notification = loadNotificationPort.loadById(command.notificationId())
                .orElseThrow(() -> new NotificationNotFoundException(command.notificationId()));

        if (!notification.getUserId().equals(command.userId())) {
            throw new NotificationNotOwnedException(command.notificationId());
        }

        notification.markRead();
        saveNotificationPort.save(notification);
    }
}
