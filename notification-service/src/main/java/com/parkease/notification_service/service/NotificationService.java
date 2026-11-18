package com.parkease.notification_service.service;

import com.parkease.notification_service.dto.request.NotificationSendRequest;
import com.parkease.notification_service.dto.response.NotificationResponse;
import com.parkease.notification_service.dto.response.UnreadCountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    // 1. Send or trigger a notification
    NotificationResponse sendNotification(NotificationSendRequest request);

    // 2. Get user's notification inbox (with optional unread-only filter)
    Page<NotificationResponse> getUserNotifications(UUID userId, boolean unreadOnly, Pageable pageable);

    // 3. Fast badge count for unread notifications
    UnreadCountResponse getUnreadCount(UUID userId);

    // 4. Mark single notification as read
    NotificationResponse markAsRead(UUID notificationId, UUID userId);

    // 5. Bulk mark all notifications as read
    void markAllAsRead(UUID userId);

    // 6. Soft-delete / dismiss notification
    void deleteNotification(UUID notificationId, UUID userId);
}
