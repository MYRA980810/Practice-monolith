package com.livecomerce.notification.infrastructure.persistence;

import com.livecomerce.notification.application.port.out.LoadNotificationPort;
import com.livecomerce.notification.application.port.out.SaveNotificationPort;
import com.livecomerce.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class NotificationPersistenceAdapter implements SaveNotificationPort, LoadNotificationPort {

    private final NotificationJpaRepository repository;

    @Override
    @SuppressWarnings("null")
    public Notification save(Notification notification) {
        return repository.save(notification);
    }

    @Override
    public Page<Notification> loadByUserId(UUID userId, Pageable pageable) {
        return repository.findByUserId(userId, pageable);
    }

    @Override
    @SuppressWarnings("null")
    public Optional<Notification> loadById(UUID id) {
        return repository.findById(id);
    }
}
