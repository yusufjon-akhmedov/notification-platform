package uz.yusufjon.notificationplatform.notification.sender;

import org.springframework.stereotype.Component;
import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;
import uz.yusufjon.notificationplatform.notification.entity.Notification;

@Component
public class PushNotificationSender implements NotificationSender {

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public NotificationSendResult send(Notification notification) {
        return NotificationSendResult.failure(
                "Mock PUSH delivery is configured to fail for testing"
        );
    }
}
