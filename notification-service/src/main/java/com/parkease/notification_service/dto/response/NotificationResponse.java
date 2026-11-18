package com.parkease.notification_service.dto.response;

import com.parkease.notification_service.enums.NotificationChannel;
import com.parkease.notification_service.enums.NotificationStatus;
import com.parkease.notification_service.enums.NotificationType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private UUID notificationId;
    private UUID userId;
    private String recipientEmail;
    private String recipientPhone;
    private NotificationType type;
    private NotificationChannel channel;
    private String title;
    private String message;
    private NotificationStatus status;
    private Boolean isRead;
    private Instant readAt;
    private String metadataJson;
    private String failureReason;
    private Instant createdAt;
}
