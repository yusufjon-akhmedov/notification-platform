package uz.yusufjon.notificationplatform.template.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uz.yusufjon.notificationplatform.common.dto.PageResponse;
import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;
import uz.yusufjon.notificationplatform.exception.BadRequestException;
import uz.yusufjon.notificationplatform.exception.ResourceNotFoundException;
import uz.yusufjon.notificationplatform.notification.repository.NotificationRepository;
import uz.yusufjon.notificationplatform.template.dto.NotificationTemplateCreateRequest;
import uz.yusufjon.notificationplatform.template.dto.NotificationTemplateResponse;
import uz.yusufjon.notificationplatform.template.dto.NotificationTemplateUpdateRequest;
import uz.yusufjon.notificationplatform.template.entity.NotificationTemplate;
import uz.yusufjon.notificationplatform.template.repository.NotificationTemplateRepository;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationTemplateService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("id", "name", "channel", "active", "createdAt", "updatedAt");

    private final NotificationTemplateRepository notificationTemplateRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public NotificationTemplateResponse create(NotificationTemplateCreateRequest request) {
        validateUniqueName(request.name(), null);

        NotificationTemplate template = new NotificationTemplate();
        template.setName(request.name());
        template.setSubject(request.subject());
        template.setBody(request.body());
        template.setChannel(request.channel());
        template.setActive(request.active() == null || request.active());

        return toResponse(notificationTemplateRepository.save(template));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationTemplateResponse> getAll(
            NotificationChannel channel,
            Boolean active,
            String name,
            Pageable pageable
    ) {
        validateSort(pageable);

        Page<NotificationTemplateResponse> page = notificationTemplateRepository.findAllByFilters(
                        channel,
                        active,
                        normalizeKeyword(name),
                        pageable
                )
                .map(this::toResponse);

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public NotificationTemplateResponse getById(Long id) {
        return toResponse(getTemplate(id));
    }

    @Transactional
    public NotificationTemplateResponse update(Long id, NotificationTemplateUpdateRequest request) {
        NotificationTemplate template = getTemplate(id);
        validateUniqueName(request.name(), id);

        template.setName(request.name());
        template.setSubject(request.subject());
        template.setBody(request.body());
        template.setChannel(request.channel());

        if (request.active() != null) {
            template.setActive(request.active());
        }

        return toResponse(notificationTemplateRepository.save(template));
    }

    @Transactional
    public void delete(Long id) {
        NotificationTemplate template = getTemplate(id);

        if (notificationRepository.existsByTemplateId(id)) {
            throw new BadRequestException("Template cannot be deleted because it is used by existing notifications");
        }

        notificationTemplateRepository.delete(template);
    }

    private NotificationTemplate getTemplate(Long id) {
        return notificationTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));
    }

    private void validateUniqueName(String name, Long id) {
        boolean exists = id == null
                ? notificationTemplateRepository.existsByName(name)
                : notificationTemplateRepository.existsByNameAndIdNot(name, id);

        if (exists) {
            throw new BadRequestException("Template name already exists");
        }
    }

    private void validateSort(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Unsupported sort field for templates: " + order.getProperty());
            }
        });
    }

    private String normalizeKeyword(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private NotificationTemplateResponse toResponse(NotificationTemplate template) {
        return new NotificationTemplateResponse(
                template.getId(),
                template.getName(),
                template.getSubject(),
                template.getBody(),
                template.getChannel(),
                template.isActive(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}
