package com.parkease.notification_service.service.impl;

import com.parkease.notification_service.dto.request.NotificationSendRequest;
import com.parkease.notification_service.dto.response.NotificationResponse;
import com.parkease.notification_service.dto.response.UnreadCountResponse;
import com.parkease.notification_service.entity.Notification;
import com.parkease.notification_service.enums.NotificationStatus;
import com.parkease.notification_service.exception.ResourceNotFoundException;
import com.parkease.notification_service.repository.NotificationRepository;
import com.parkease.notification_service.service.NotificationService;
import com.parkease.notification_service.service.dispatcher.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDispatcher notificationDispatcher;

    @Override
    @Transactional
    public NotificationResponse sendNotification(NotificationSendRequest request) {
        log.info("Processing notification dispatch for userId: {} | Type: {} | Channel: {}",
                request.getUserId(), request.getType(), request.getChannel());

        // 1. Build initial notification entity in PENDING status
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .recipientEmail(request.getRecipientEmail())
                .recipientPhone(request.getRecipientPhone())
                .type(request.getType())
                .channel(request.getChannel())
                .title(request.getTitle())
                .message(request.getMessage())
                .metadataJson(request.getMetadataJson())
                .status(NotificationStatus.PENDING)
                .isRead(false)
                .isActive(true)
                .build();

        // 2. Dispatch alert via selected channel (Email, SMS, In-App, Push)
        notificationDispatcher.dispatch(notification);

        // 3. Save to database (preserves SENT or FAILED audit trail)
        Notification saved = notificationRepository.save(notification);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(UUID userId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByUserIdAndIsReadFalseAndIsActiveTrueOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId, pageable);

        return page.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID userId) {
        long count = notificationRepository.countByUserIdAndIsReadFalseAndIsActiveTrue(userId);
        return new UnreadCountResponse(count);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findByNotificationIdAndUserIdAndIsActiveTrue(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }

        return mapToResponse(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        int updated = notificationRepository.markAllAsReadForUser(userId, Instant.now());
        log.info("Marked {} notifications as read for userId: {}", updated, userId);
    }

    @Override
    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findByNotificationIdAndUserIdAndIsActiveTrue(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));

        notification.setIsActive(false); // Soft delete
        notificationRepository.save(notification);
        log.info("Dismissed notification {} for userId: {}", notificationId, userId);
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .userId(n.getUserId())
                .recipientEmail(n.getRecipientEmail())
                .recipientPhone(n.getRecipientPhone())
                .type(n.getType())
                .channel(n.getChannel())
                .title(n.getTitle())
                .message(n.getMessage())
                .status(n.getStatus())
                .isRead(n.getIsRead())
                .readAt(n.getReadAt())
                .metadataJson(n.getMetadataJson())
                .failureReason(n.getFailureReason())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
