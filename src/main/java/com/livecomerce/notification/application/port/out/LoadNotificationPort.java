package com.livecomerce.notification.application.port.out;

import com.livecomerce.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface LoadNotificationPort {

    Page<Notification> loadByUserId(UUID userId, Pageable pageable);

    Optional<Notification> loadById(UUID id);
}
