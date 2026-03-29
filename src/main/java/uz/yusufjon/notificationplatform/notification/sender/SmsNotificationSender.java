package uz.yusufjon.notificationplatform.notification.sender;

import org.springframework.stereotype.Component;
import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;
import uz.yusufjon.notificationplatform.notification.entity.Notification;

@Component
public class SmsNotificationSender implements NotificationSender {

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public NotificationSendResult send(Notification notification) {
        return NotificationSendResult.success(
                "Mock SMS delivery accepted for recipient " + notification.getRecipient()
        );
    }
}
