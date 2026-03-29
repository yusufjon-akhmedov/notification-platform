package uz.yusufjon.notificationplatform.template.dto;

import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;

import java.time.LocalDateTime;

public record NotificationTemplateResponse(
        Long id,
        String name,
        String subject,
        String body,
        NotificationChannel channel,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
