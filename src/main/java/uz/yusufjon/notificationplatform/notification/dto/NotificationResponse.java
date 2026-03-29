package uz.yusufjon.notificationplatform.notification.dto;

import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;
import uz.yusufjon.notificationplatform.common.enums.NotificationStatus;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String recipient,
        String subject,
        String content,
        NotificationChannel channel,
        NotificationStatus status,
        Long templateId,
        String templateName,
        String createdByEmail,
        LocalDateTime sentAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
