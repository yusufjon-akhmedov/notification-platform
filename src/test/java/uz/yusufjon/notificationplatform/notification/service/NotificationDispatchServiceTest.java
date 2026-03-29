package uz.yusufjon.notificationplatform.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;
import uz.yusufjon.notificationplatform.common.enums.NotificationStatus;
import uz.yusufjon.notificationplatform.common.enums.RoleName;
import uz.yusufjon.notificationplatform.exception.BadRequestException;
import uz.yusufjon.notificationplatform.notification.dto.NotificationResponse;
import uz.yusufjon.notificationplatform.notification.entity.Notification;
import uz.yusufjon.notificationplatform.notification.entity.NotificationDeliveryAttempt;
import uz.yusufjon.notificationplatform.notification.repository.NotificationDeliveryAttemptRepository;
import uz.yusufjon.notificationplatform.notification.repository.NotificationRepository;
import uz.yusufjon.notificationplatform.notification.sender.NotificationSendResult;
import uz.yusufjon.notificationplatform.notification.sender.NotificationSender;
import uz.yusufjon.notificationplatform.template.repository.NotificationTemplateRepository;
import uz.yusufjon.notificationplatform.user.entity.Role;
import uz.yusufjon.notificationplatform.user.entity.User;
import uz.yusufjon.notificationplatform.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDeliveryAttemptRepository notificationDeliveryAttemptRepository;

    @Mock
    private NotificationTemplateRepository notificationTemplateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationSender emailSender;

    @Mock
    private NotificationSender pushSender;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                notificationDeliveryAttemptRepository,
                notificationTemplateRepository,
                userRepository,
                List.of(emailSender, pushSender)
        );
    }

    @Test
    void dispatchShouldCreateSuccessfulAttemptAndMarkNotificationAsSent() {
        User operator = user(1L, "operator@example.com", RoleName.OPERATOR);
        Notification notification = notification(50L, operator, NotificationStatus.PENDING, NotificationChannel.EMAIL);

        when(userRepository.findByEmail(operator.getEmail())).thenReturn(Optional.of(operator));
        when(notificationRepository.findDetailedById(50L)).thenReturn(Optional.of(notification));
        when(emailSender.getChannel()).thenReturn(NotificationChannel.EMAIL);
        when(emailSender.send(notification)).thenReturn(NotificationSendResult.success("mock-email-ok"));
        when(notificationDeliveryAttemptRepository.findTopByNotificationIdOrderByAttemptNumberDesc(50L)).thenReturn(Optional.empty());
        when(notificationDeliveryAttemptRepository.save(any(NotificationDeliveryAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.dispatch(50L, operator.getEmail());

        ArgumentCaptor<NotificationDeliveryAttempt> attemptCaptor =
                ArgumentCaptor.forClass(NotificationDeliveryAttempt.class);
        verify(notificationDeliveryAttemptRepository).save(attemptCaptor.capture());

        NotificationDeliveryAttempt attempt = attemptCaptor.getValue();
        assertThat(attempt.getAttemptNumber()).isEqualTo(1);
        assertThat(attempt.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(attempt.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(attempt.getProviderResponse()).isEqualTo("mock-email-ok");
        assertThat(attempt.getErrorMessage()).isNull();
        assertThat(attempt.getAttemptedAt()).isNotNull();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getSentAt()).isEqualTo(attempt.getAttemptedAt());
        assertThat(response.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(response.sentAt()).isEqualTo(attempt.getAttemptedAt());
    }

    @Test
    void dispatchShouldCreateFailedAttemptAndMarkNotificationAsFailed() {
        User operator = user(1L, "operator@example.com", RoleName.OPERATOR);
        Notification notification = notification(51L, operator, NotificationStatus.PENDING, NotificationChannel.PUSH);
        NotificationDeliveryAttempt previousAttempt = new NotificationDeliveryAttempt();
        previousAttempt.setAttemptNumber(1);

        when(userRepository.findByEmail(operator.getEmail())).thenReturn(Optional.of(operator));
        when(notificationRepository.findDetailedById(51L)).thenReturn(Optional.of(notification));
        when(emailSender.getChannel()).thenReturn(NotificationChannel.EMAIL);
        when(pushSender.getChannel()).thenReturn(NotificationChannel.PUSH);
        when(pushSender.send(notification)).thenReturn(NotificationSendResult.failure("Mock PUSH delivery is configured to fail for testing"));
        when(notificationDeliveryAttemptRepository.findTopByNotificationIdOrderByAttemptNumberDesc(51L))
                .thenReturn(Optional.of(previousAttempt));
        when(notificationDeliveryAttemptRepository.save(any(NotificationDeliveryAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.dispatch(51L, operator.getEmail());

        ArgumentCaptor<NotificationDeliveryAttempt> attemptCaptor =
                ArgumentCaptor.forClass(NotificationDeliveryAttempt.class);
        verify(notificationDeliveryAttemptRepository).save(attemptCaptor.capture());

        NotificationDeliveryAttempt attempt = attemptCaptor.getValue();
        assertThat(attempt.getAttemptNumber()).isEqualTo(2);
        assertThat(attempt.getChannel()).isEqualTo(NotificationChannel.PUSH);
        assertThat(attempt.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(attempt.getProviderResponse()).isNull();
        assertThat(attempt.getErrorMessage()).isEqualTo("Mock PUSH delivery is configured to fail for testing");
        assertThat(attempt.getAttemptedAt()).isNotNull();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getSentAt()).isNull();
        assertThat(response.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(response.sentAt()).isNull();
    }

    @Test
    void dispatchShouldRejectNonPendingNotification() {
        User operator = user(1L, "operator@example.com", RoleName.OPERATOR);
        Notification notification = notification(52L, operator, NotificationStatus.SENT, NotificationChannel.EMAIL);

        when(userRepository.findByEmail(operator.getEmail())).thenReturn(Optional.of(operator));
        when(notificationRepository.findDetailedById(52L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.dispatch(52L, operator.getEmail()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only pending notifications can be dispatched");

        verify(notificationDeliveryAttemptRepository, never()).save(any(NotificationDeliveryAttempt.class));
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(emailSender, never()).send(any(Notification.class));
        verify(pushSender, never()).send(any(Notification.class));
    }

    private User user(Long id, String email, RoleName roleName) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setEnabled(true);

        Role role = new Role();
        role.setName(roleName);
        user.setRole(role);

        return user;
    }

    private Notification notification(Long id, User createdBy, NotificationStatus status, NotificationChannel channel) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setRecipient("recipient@example.com");
        notification.setSubject("Subject");
        notification.setContent("Content");
        notification.setStatus(status);
        notification.setChannel(channel);
        notification.setCreatedBy(createdBy);
        return notification;
    }
}
