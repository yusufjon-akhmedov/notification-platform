package uz.yusufjon.notificationplatform.notification.sender;

import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;
import uz.yusufjon.notificationplatform.notification.entity.Notification;

public interface NotificationSender {

    NotificationChannel getChannel();

    NotificationSendResult send(Notification notification);
}
