package uz.yusufjon.notificationplatform.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;

public record NotificationTemplateUpdateRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Size(max = 255, message = "Subject must be at most 255 characters")
        String subject,

        @NotBlank(message = "Body is required")
        String body,

        @NotNull(message = "Channel is required")
        NotificationChannel channel,

        Boolean active
) {
}
