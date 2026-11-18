package com.parkease.notification_service.dto.request;

import com.parkease.notification_service.enums.NotificationChannel;
import com.parkease.notification_service.enums.NotificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSendRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @Email(message = "Recipient email must be a valid email address")
    private String recipientEmail;

    private String recipientPhone;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotNull(message = "Notification channel is required")
    @Builder.Default
    private NotificationChannel channel = NotificationChannel.IN_APP;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private String metadataJson;
}
