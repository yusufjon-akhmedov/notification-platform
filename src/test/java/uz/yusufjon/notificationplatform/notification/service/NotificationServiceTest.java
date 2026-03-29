package uz.yusufjon.notificationplatform.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uz.yusufjon.notificationplatform.common.dto.PageResponse;
import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;
import uz.yusufjon.notificationplatform.common.enums.NotificationStatus;
import uz.yusufjon.notificationplatform.common.enums.RoleName;
import uz.yusufjon.notificationplatform.exception.BadRequestException;
import uz.yusufjon.notificationplatform.exception.ForbiddenException;
import uz.yusufjon.notificationplatform.notification.dto.NotificationCreateRequest;
import uz.yusufjon.notificationplatform.notification.dto.NotificationResponse;
import uz.yusufjon.notificationplatform.notification.entity.Notification;
import uz.yusufjon.notificationplatform.notification.repository.NotificationDeliveryAttemptRepository;
import uz.yusufjon.notificationplatform.notification.repository.NotificationRepository;
import uz.yusufjon.notificationplatform.notification.sender.NotificationSender;
import uz.yusufjon.notificationplatform.template.entity.NotificationTemplate;
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
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDeliveryAttemptRepository notificationDeliveryAttemptRepository;

    @Mock
    private NotificationTemplateRepository notificationTemplateRepository;

    @Mock
    private UserRepository userRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                notificationDeliveryAttemptRepository,
                notificationTemplateRepository,
                userRepository,
                List.<NotificationSender>of()
        );
    }

    @Test
    void createShouldPersistDirectNotificationForCurrentUser() {
        User operator = user(1L, "operator@example.com", RoleName.OPERATOR);
        NotificationCreateRequest request = new NotificationCreateRequest(
                "user@example.com",
                " ",
                "Welcome to the platform",
                NotificationChannel.EMAIL,
                null
        );

        when(userRepository.findByEmail(operator.getEmail())).thenReturn(Optional.of(operator));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(100L);
            return notification;
        });

        NotificationResponse response = notificationService.create(request, operator.getEmail());

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.recipient()).isEqualTo("user@example.com");
        assertThat(response.subject()).isNull();
        assertThat(response.content()).isEqualTo("Welcome to the platform");
        assertThat(response.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(response.status()).isEqualTo(NotificationStatus.PENDING);
        assertThat(response.createdByEmail()).isEqualTo(operator.getEmail());
        assertThat(response.templateId()).isNull();
    }

    @Test
    void createShouldPersistNotificationFromTemplate() {
        User operator = user(1L, "operator@example.com", RoleName.OPERATOR);
        NotificationTemplate template = template(20L, "Welcome Template", NotificationChannel.SMS, true);
        template.setSubject("Welcome");
        template.setBody("Template body");
        NotificationCreateRequest request = new NotificationCreateRequest(
                "998901234567",
                null,
                null,
                null,
                20L
        );

        when(userRepository.findByEmail(operator.getEmail())).thenReturn(Optional.of(operator));
        when(notificationTemplateRepository.findById(20L)).thenReturn(Optional.of(template));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(101L);
            return notification;
        });

        NotificationResponse response = notificationService.create(request, operator.getEmail());

        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.recipient()).isEqualTo("998901234567");
        assertThat(response.subject()).isEqualTo("Welcome");
        assertThat(response.content()).isEqualTo("Template body");
        assertThat(response.channel()).isEqualTo(NotificationChannel.SMS);
        assertThat(response.status()).isEqualTo(NotificationStatus.PENDING);
        assertThat(response.templateId()).isEqualTo(20L);
        assertThat(response.templateName()).isEqualTo("Welcome Template");
    }

    @Test
    void cancelShouldMarkPendingNotificationAsCancelled() {
        User operator = user(1L, "operator@example.com", RoleName.OPERATOR);
        Notification notification = notification(5L, operator, NotificationStatus.PENDING, NotificationChannel.EMAIL);

        when(userRepository.findByEmail(operator.getEmail())).thenReturn(Optional.of(operator));
        when(notificationRepository.findDetailedById(5L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.cancel(5L, operator.getEmail());

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
        assertThat(response.status()).isEqualTo(NotificationStatus.CANCELLED);
        verify(notificationRepository).save(notification);
    }

    @Test
    void cancelShouldRejectNonPendingNotification() {
        User operator = user(1L, "operator@example.com", RoleName.OPERATOR);
        Notification notification = notification(5L, operator, NotificationStatus.SENT, NotificationChannel.EMAIL);

        when(userRepository.findByEmail(operator.getEmail())).thenReturn(Optional.of(operator));
        when(notificationRepository.findDetailedById(5L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.cancel(5L, operator.getEmail()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only pending notifications can be cancelled");

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void getAccessibleNotificationShouldRejectOperatorAccessingAnotherUsersNotification() {
        User operator = user(1L, "operator@example.com", RoleName.OPERATOR);
        User otherOperator = user(2L, "other@example.com", RoleName.OPERATOR);
        Notification notification = notification(12L, otherOperator, NotificationStatus.PENDING, NotificationChannel.EMAIL);

        when(userRepository.findByEmail(operator.getEmail())).thenReturn(Optional.of(operator));
        when(notificationRepository.findDetailedById(12L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.getAccessibleNotification(12L, operator.getEmail()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("You do not have permission to access this notification");
    }

    @Test
    void getAllShouldScopeNotificationsToCurrentOperator() {
        User operator = user(1L, "operator@example.com", RoleName.OPERATOR);
        Pageable pageable = PageRequest.of(0, 10);
        Notification notification = notification(22L, operator, NotificationStatus.PENDING, NotificationChannel.EMAIL);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);

        when(userRepository.findByEmail(operator.getEmail())).thenReturn(Optional.of(operator));
        when(notificationRepository.findAllByFilters(
                NotificationStatus.PENDING,
                NotificationChannel.EMAIL,
                "gmail",
                operator.getId(),
                pageable
        )).thenReturn(page);

        PageResponse<NotificationResponse> response = notificationService.getAll(
                NotificationStatus.PENDING,
                NotificationChannel.EMAIL,
                "gmail",
                pageable,
                operator.getEmail()
        );

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        verify(notificationRepository).findAllByFilters(
                NotificationStatus.PENDING,
                NotificationChannel.EMAIL,
                "gmail",
                operator.getId(),
                pageable
        );
    }

    @Test
    void getAllShouldAllowAdminToViewAllNotifications() {
        User admin = user(99L, "admin@example.com", RoleName.ADMIN);
        Pageable pageable = PageRequest.of(0, 10);
        Notification notification = notification(33L, user(1L, "operator@example.com", RoleName.OPERATOR), NotificationStatus.SENT, NotificationChannel.SMS);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);

        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(notificationRepository.findAllByFilters(
                NotificationStatus.SENT,
                NotificationChannel.SMS,
                "998",
                null,
                pageable
        )).thenReturn(page);

        PageResponse<NotificationResponse> response = notificationService.getAll(
                NotificationStatus.SENT,
                NotificationChannel.SMS,
                "998",
                pageable,
                admin.getEmail()
        );

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().getFirst().createdByEmail()).isEqualTo("operator@example.com");
        verify(notificationRepository).findAllByFilters(
                NotificationStatus.SENT,
                NotificationChannel.SMS,
                "998",
                null,
                pageable
        );
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

    private NotificationTemplate template(Long id, String name, NotificationChannel channel, boolean active) {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(id);
        template.setName(name);
        template.setChannel(channel);
        template.setActive(active);
        return template;
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
