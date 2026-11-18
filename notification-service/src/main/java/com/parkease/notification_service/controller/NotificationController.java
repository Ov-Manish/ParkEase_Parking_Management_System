package com.parkease.notification_service.controller;

import com.parkease.notification_service.dto.request.NotificationSendRequest;
import com.parkease.notification_service.dto.response.ApiResponse;
import com.parkease.notification_service.dto.response.NotificationResponse;
import com.parkease.notification_service.dto.response.UnreadCountResponse;
import com.parkease.notification_service.exception.BadRequestException;
import com.parkease.notification_service.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Management", description = "Endpoints for user notification inbox, unread badges, and alert dispatching")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    // 1. Get User Notification Inbox (Paged, newest-first, optional unreadOnly filter)
    @GetMapping
    @Operation(summary = "Get User Notifications", description = "Retrieves paginated notifications for the authenticated user, sorted newest first.")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getUserNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        Pageable pageable = PageRequest.of(page, size);

        Page<NotificationResponse> response = notificationService.getUserNotifications(userId, unreadOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", response));
    }

    // 2. Get Fast Unread Count Badge
    @GetMapping("/unread-count")
    @Operation(summary = "Get Unread Count Badge", description = "Returns the total number of unread notifications for badge counters.")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        UnreadCountResponse response = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved successfully", response));
    }

    // 3. Mark Single Notification as Read
    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark Notification as Read", description = "Marks a specific notification as read by ID.")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable UUID notificationId,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        NotificationResponse response = notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", response));
    }

    // 4. Mark All Notifications as Read
    @PatchMapping("/read-all")
    @Operation(summary = "Mark All Notifications as Read", description = "Marks all unread notifications for the user as read in bulk.")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read"));
    }

    // 5. Soft-Delete / Dismiss Notification
    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Dismiss / Delete Notification", description = "Soft-deletes a notification from the user's inbox.")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable UUID notificationId,
            Authentication authentication) {

        UUID userId = extractUserId(authentication);
        notificationService.deleteNotification(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification dismissed successfully"));
    }

    // 6. Direct Trigger / Send Notification (For Testing & Inter-Service REST)
    @PostMapping("/send")
    @Operation(summary = "Send Notification (Direct / Test)", description = "Dispatches a notification directly via Email, SMS, or In-App. Useful for Postman testing and inter-service alerts.")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(@Valid @RequestBody NotificationSendRequest request) {
        log.info("REST request to send notification to userId: {} via channel: {}", request.getUserId(), request.getChannel());
        NotificationResponse response = notificationService.sendNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Notification sent successfully", response));
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BadRequestException("User authentication required");
        }
        try {
            return UUID.fromString(authentication.getPrincipal().toString());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid user ID in authentication token");
        }
    }
}
