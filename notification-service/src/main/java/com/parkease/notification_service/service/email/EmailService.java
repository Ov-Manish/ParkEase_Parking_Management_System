package com.parkease.notification_service.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an email via JavaMailSender if configured, or logs the email body if credentials are empty.
     */
    public void sendEmail(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Recipient email address cannot be empty");
        }

        // If credentials are configured in application.yml, send real email!
        if (mailSender != null && senderEmail != null && !senderEmail.isBlank()) {
            try {
                log.info("Sending REAL email via SMTP to: {} with subject: '{}'", to, subject);
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(senderEmail);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                log.info("Email delivered successfully to: {}", to);
                return;
            } catch (Exception ex) {
                log.error("Failed to send real email via SMTP to {}: {}. Falling back to console dispatch.", to, ex.getMessage());
            }
        }

        // Fallback / Development simulation
        log.info("""
                \n====================== [DISPATCH - EMAIL (DEV SIMULATION)] ======================
                To:      {}
                From:    {}
                Subject: {}
                Body:
                {}
                =================================================================================\
                """, to, (senderEmail != null && !senderEmail.isBlank() ? senderEmail : "noreply@parkease.com"), subject, body);
    }
}
