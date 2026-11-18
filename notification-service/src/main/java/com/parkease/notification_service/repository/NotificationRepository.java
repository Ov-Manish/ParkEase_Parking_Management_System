package com.parkease.notification_service.repository;

import com.parkease.notification_service.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // 1. Paged notifications for user (newest first)
    Page<Notification> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // 2. Filter unread only
    Page<Notification> findByUserIdAndIsReadFalseAndIsActiveTrueOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // 3. Fast unread count badge
    long countByUserIdAndIsReadFalseAndIsActiveTrue(UUID userId);

    // 4. Find single active notification by user and ID
    Optional<Notification> findByNotificationIdAndUserIdAndIsActiveTrue(UUID notificationId, UUID userId);

    // 5. Bulk mark all unread as read
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.isRead = false AND n.isActive = true")
    int markAllAsReadForUser(@Param("userId") UUID userId, @Param("readAt") Instant readAt);
}
