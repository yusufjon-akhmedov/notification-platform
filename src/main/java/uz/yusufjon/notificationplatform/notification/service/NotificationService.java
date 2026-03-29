package uz.yusufjon.notificationplatform.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uz.yusufjon.notificationplatform.common.dto.PageResponse;
import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;
import uz.yusufjon.notificationplatform.common.enums.NotificationStatus;
import uz.yusufjon.notificationplatform.common.enums.RoleName;
import uz.yusufjon.notificationplatform.exception.BadRequestException;
import uz.yusufjon.notificationplatform.exception.ForbiddenException;
import uz.yusufjon.notificationplatform.exception.ResourceNotFoundException;
import uz.yusufjon.notificationplatform.notification.dto.NotificationCreateRequest;
import uz.yusufjon.notificationplatform.notification.dto.NotificationResponse;
import uz.yusufjon.notificationplatform.notification.entity.Notification;
import uz.yusufjon.notificationplatform.notification.entity.NotificationDeliveryAttempt;
import uz.yusufjon.notificationplatform.notification.repository.NotificationDeliveryAttemptRepository;
import uz.yusufjon.notificationplatform.notification.repository.NotificationRepository;
import uz.yusufjon.notificationplatform.notification.sender.NotificationSendResult;
import uz.yusufjon.notificationplatform.notification.sender.NotificationSender;
import uz.yusufjon.notificationplatform.template.entity.NotificationTemplate;
import uz.yusufjon.notificationplatform.template.repository.NotificationTemplateRepository;
import uz.yusufjon.notificationplatform.user.entity.User;
import uz.yusufjon.notificationplatform.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "recipient", "status", "channel", "sentAt", "createdAt", "updatedAt");

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryAttemptRepository notificationDeliveryAttemptRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final UserRepository userRepository;
    private final List<NotificationSender> notificationSenders;

    @Transactional
    public NotificationResponse create(NotificationCreateRequest request, String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);

        Notification notification = new Notification();
        notification.setRecipient(request.recipient());
        notification.setStatus(NotificationStatus.PENDING);
        notification.setCreatedBy(currentUser);
        notification.setSentAt(null);

        applyContent(notification, request);

        return toResponse(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getAll(
            NotificationStatus status,
            NotificationChannel channel,
            String recipient,
            Pageable pageable,
            String currentUserEmail
    ) {
        validateSort(pageable);

        User currentUser = getUserByEmail(currentUserEmail);
        Long createdById = hasGlobalAccess(currentUser) ? null : currentUser.getId();

        Page<NotificationResponse> page = notificationRepository.findAllByFilters(
                        status,
                        channel,
                        normalizeKeyword(recipient),
                        createdById,
                        pageable
                )
                .map(this::toResponse);

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public NotificationResponse getById(Long id, String currentUserEmail) {
        return toResponse(getAccessibleNotification(id, currentUserEmail));
    }

    @Transactional
    public NotificationResponse cancel(Long id, String currentUserEmail) {
        Notification notification = getAccessibleNotification(id, currentUserEmail);

        if (notification.getStatus() != NotificationStatus.PENDING) {
            throw new BadRequestException("Only pending notifications can be cancelled");
        }

        notification.setStatus(NotificationStatus.CANCELLED);

        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public NotificationResponse dispatch(Long id, String currentUserEmail) {
        Notification notification = getAccessibleNotification(id, currentUserEmail);

        if (notification.getStatus() != NotificationStatus.PENDING) {
            throw new BadRequestException("Only pending notifications can be dispatched");
        }

        NotificationSender sender = getSender(notification.getChannel());
        NotificationSendResult result = sender.send(notification);
        NotificationDeliveryAttempt attempt = buildAttempt(notification, result);

        notificationDeliveryAttemptRepository.save(attempt);

        if (result.success()) {
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(attempt.getAttemptedAt());
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setSentAt(null);
        }

        return toResponse(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public Notification getAccessibleNotification(Long id, String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        Notification notification = getNotification(id);

        validateAccess(notification, currentUser);

        return notification;
    }

    private void applyContent(Notification notification, NotificationCreateRequest request) {
        boolean hasTemplate = request.templateId() != null;
        boolean hasDirectContent = StringUtils.hasText(request.content()) || request.channel() != null || StringUtils.hasText(request.subject());

        if (hasTemplate && hasDirectContent) {
            throw new BadRequestException("Provide either templateId or direct content fields, not both");
        }

        if (hasTemplate) {
            NotificationTemplate template = notificationTemplateRepository.findById(request.templateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + request.templateId()));

            if (!template.isActive()) {
                throw new BadRequestException("Inactive templates cannot be used to create notifications");
            }

            notification.setTemplate(template);
            notification.setSubject(template.getSubject());
            notification.setContent(template.getBody());
            notification.setChannel(template.getChannel());
            return;
        }

        if (!StringUtils.hasText(request.content()) || request.channel() == null) {
            throw new BadRequestException("Direct notifications require content and channel");
        }

        notification.setTemplate(null);
        notification.setSubject(normalizeNullable(request.subject()));
        notification.setContent(request.content());
        notification.setChannel(request.channel());
    }

    private Notification getNotification(Long id) {
        return notificationRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private void validateAccess(Notification notification, User currentUser) {
        if (hasGlobalAccess(currentUser)) {
            return;
        }

        if (notification.getCreatedBy() == null || !notification.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to access this notification");
        }
    }

    private boolean hasGlobalAccess(User user) {
        RoleName roleName = user.getRole().getName();
        return roleName == RoleName.ADMIN || roleName == RoleName.MANAGER;
    }

    private NotificationSender getSender(NotificationChannel channel) {
        return notificationSenders.stream()
                .filter(sender -> sender.getChannel() == channel)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No sender configured for channel: " + channel));
    }

    private NotificationDeliveryAttempt buildAttempt(Notification notification, NotificationSendResult result) {
        NotificationDeliveryAttempt attempt = new NotificationDeliveryAttempt();
        LocalDateTime attemptedAt = LocalDateTime.now();

        attempt.setNotification(notification);
        attempt.setAttemptNumber(getNextAttemptNumber(notification.getId()));
        attempt.setChannel(notification.getChannel());
        attempt.setStatus(result.success() ? NotificationStatus.SENT : NotificationStatus.FAILED);
        attempt.setErrorMessage(result.errorMessage());
        attempt.setAttemptedAt(attemptedAt);
        attempt.setProviderResponse(result.providerResponse());

        return attempt;
    }

    private int getNextAttemptNumber(Long notificationId) {
        return notificationDeliveryAttemptRepository.findTopByNotificationIdOrderByAttemptNumberDesc(notificationId)
                .map(existingAttempt -> existingAttempt.getAttemptNumber() + 1)
                .orElse(1);
    }

    private void validateSort(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Unsupported sort field for notifications: " + order.getProperty());
            }
        });
    }

    private String normalizeKeyword(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private NotificationResponse toResponse(Notification notification) {
        Long templateId = notification.getTemplate() != null ? notification.getTemplate().getId() : null;
        String templateName = notification.getTemplate() != null ? notification.getTemplate().getName() : null;
        String createdByEmail = notification.getCreatedBy() != null ? notification.getCreatedBy().getEmail() : null;

        return new NotificationResponse(
                notification.getId(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getContent(),
                notification.getChannel(),
                notification.getStatus(),
                templateId,
                templateName,
                createdByEmail,
                notification.getSentAt(),
                notification.getCreatedAt(),
                notification.getUpdatedAt()
        );
    }
}
