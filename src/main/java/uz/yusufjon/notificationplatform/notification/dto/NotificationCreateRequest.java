package uz.yusufjon.notificationplatform.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;

public record NotificationCreateRequest(

        @NotBlank(message = "Recipient is required")
        @Size(max = 150, message = "Recipient must be at most 150 characters")
        String recipient,

        @Size(max = 255, message = "Subject must be at most 255 characters")
        String subject,

        String content,

        NotificationChannel channel,

        Long templateId
) {
}
