package uz.yusufjon.notificationplatform.notification.dto;

import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;
import uz.yusufjon.notificationplatform.common.enums.NotificationStatus;

import java.time.LocalDateTime;

public record NotificationDeliveryAttemptResponse(
        Long id,
        Integer attemptNumber,
        NotificationChannel channel,
        NotificationStatus status,
        String errorMessage,
        LocalDateTime attemptedAt,
        String providerResponse,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
