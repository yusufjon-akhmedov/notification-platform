package uz.yusufjon.notificationplatform.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.yusufjon.notificationplatform.notification.entity.NotificationDeliveryAttempt;

import java.util.List;
import java.util.Optional;

public interface NotificationDeliveryAttemptRepository extends JpaRepository<NotificationDeliveryAttempt, Long> {
    List<NotificationDeliveryAttempt> findByNotificationIdOrderByAttemptNumberAscAttemptedAtAsc(Long notificationId);

    Optional<NotificationDeliveryAttempt> findTopByNotificationIdOrderByAttemptNumberDesc(Long notificationId);
}
