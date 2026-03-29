package uz.yusufjon.notificationplatform.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;
import uz.yusufjon.notificationplatform.common.enums.NotificationStatus;
import uz.yusufjon.notificationplatform.common.enums.RoleName;
import uz.yusufjon.notificationplatform.exception.BadRequestException;
import uz.yusufjon.notificationplatform.exception.ForbiddenException;
import uz.yusufjon.notificationplatform.exception.ResourceNotFoundException;
import uz.yusufjon.notificationplatform.notification.dto.NotificationCreateRequest;
import uz.yusufjon.notificationplatform.notification.dto.NotificationResponse;
import uz.yusufjon.notificationplatform.notification.entity.Notification;
import uz.yusufjon.notificationplatform.notification.repository.NotificationRepository;
import uz.yusufjon.notificationplatform.template.entity.NotificationTemplate;
import uz.yusufjon.notificationplatform.template.repository.NotificationTemplateRepository;
import uz.yusufjon.notificationplatform.user.entity.User;
import uz.yusufjon.notificationplatform.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final UserRepository userRepository;

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
    public List<NotificationResponse> getAll(
            NotificationStatus status,
            NotificationChannel channel,
            String currentUserEmail
    ) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long createdById = hasGlobalAccess(currentUser) ? null : currentUser.getId();

        return notificationRepository.findAllByFilters(status, channel, createdById)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationResponse getById(Long id, String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        Notification notification = getNotification(id);

        validateAccess(notification, currentUser);

        return toResponse(notification);
    }

    @Transactional
    public NotificationResponse cancel(Long id, String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        Notification notification = getNotification(id);

        validateAccess(notification, currentUser);

        if (notification.getStatus() != NotificationStatus.PENDING) {
            throw new BadRequestException("Only pending notifications can be cancelled");
        }

        notification.setStatus(NotificationStatus.CANCELLED);

        return toResponse(notificationRepository.save(notification));
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
