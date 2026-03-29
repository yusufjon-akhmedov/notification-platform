package uz.yusufjon.notificationplatform.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.yusufjon.notificationplatform.notification.dto.NotificationDeliveryAttemptResponse;
import uz.yusufjon.notificationplatform.notification.entity.Notification;
import uz.yusufjon.notificationplatform.notification.entity.NotificationDeliveryAttempt;
import uz.yusufjon.notificationplatform.notification.repository.NotificationDeliveryAttemptRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryAttemptService {

    private final NotificationService notificationService;
    private final NotificationDeliveryAttemptRepository notificationDeliveryAttemptRepository;

    @Transactional(readOnly = true)
    public List<NotificationDeliveryAttemptResponse> getByNotificationId(Long notificationId, String currentUserEmail) {
        Notification notification = notificationService.getAccessibleNotification(notificationId, currentUserEmail);

        return notificationDeliveryAttemptRepository.findByNotificationIdOrderByAttemptNumberAscAttemptedAtAsc(notification.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private NotificationDeliveryAttemptResponse toResponse(NotificationDeliveryAttempt attempt) {
        return new NotificationDeliveryAttemptResponse(
                attempt.getId(),
                attempt.getAttemptNumber(),
                attempt.getChannel(),
                attempt.getStatus(),
                attempt.getErrorMessage(),
                attempt.getAttemptedAt(),
                attempt.getProviderResponse(),
                attempt.getCreatedAt(),
                attempt.getUpdatedAt()
        );
    }
}
