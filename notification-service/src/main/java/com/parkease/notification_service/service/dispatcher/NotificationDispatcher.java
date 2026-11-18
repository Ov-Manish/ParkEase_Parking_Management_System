package com.parkease.notification_service.service.dispatcher;

import com.parkease.notification_service.entity.Notification;
import com.parkease.notification_service.enums.NotificationStatus;
import com.parkease.notification_service.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final EmailService emailService;

    /**
     * Dispatches notification to the specified channel.
     * Returns true if dispatch succeeded, false if failed.
     */
    public boolean dispatch(Notification notification) {
        try {
            switch (notification.getChannel()) {
                case IN_APP -> dispatchInApp(notification);
                case EMAIL -> dispatchEmail(notification);
                case SMS -> dispatchSms(notification);
                case PUSH -> dispatchPush(notification);
            }
            notification.setStatus(NotificationStatus.SENT);
            return true;
        } catch (Exception ex) {
            log.error("Failed to dispatch notification {} via {}: {}",
                    notification.getNotificationId(), notification.getChannel(), ex.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason(ex.getMessage());
            return false;
        }
    }

    private void dispatchInApp(Notification notification) {
        log.info("[DISPATCH - IN_APP] Persisting alert for userId: {} | Title: '{}'",
                notification.getUserId(), notification.getTitle());
    }

    private void dispatchEmail(Notification notification) {
        emailService.sendEmail(
                notification.getRecipientEmail(),
                notification.getTitle(),
                notification.getMessage()
        );
    }

    private void dispatchSms(Notification notification) {
        String phone = notification.getRecipientPhone();
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Recipient phone number is missing for SMS channel");
        }

        log.info("""
                \n---------------------- [DISPATCH - SMS (MOCK SIMULATION)] ----------------------
                To:   {}
                Text: {} - {}
                --------------------------------------------------------------------------------\
                """, phone, notification.getTitle(), notification.getMessage());
    }

    private void dispatchPush(Notification notification) {
        log.info("[DISPATCH - PUSH] Pushing notification to mobile device for userId: {} | Title: '{}'",
                notification.getUserId(), notification.getTitle());
    }
}
