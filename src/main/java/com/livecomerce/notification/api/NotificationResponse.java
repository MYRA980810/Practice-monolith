package com.livecomerce.notification.api;

import com.livecomerce.notification.domain.Notification;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        String type,
        UUID liveId,
        Map<String, Object> payload,
        boolean isRead,
        OffsetDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getUserId(),
                n.getType(),
                n.getLiveId(),
                n.getPayload(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
