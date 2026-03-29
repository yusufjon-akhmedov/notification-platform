package uz.yusufjon.notificationplatform.notification.sender;

import org.springframework.stereotype.Component;
import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;
import uz.yusufjon.notificationplatform.notification.entity.Notification;

@Component
public class EmailNotificationSender implements NotificationSender {

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public NotificationSendResult send(Notification notification) {
        return NotificationSendResult.success(
                "Mock EMAIL delivery accepted for recipient " + notification.getRecipient()
        );
    }
}
