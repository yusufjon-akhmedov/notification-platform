package uz.yusufjon.notificationplatform.template.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;
import uz.yusufjon.notificationplatform.exception.BadRequestException;
import uz.yusufjon.notificationplatform.template.dto.NotificationTemplateCreateRequest;
import uz.yusufjon.notificationplatform.template.dto.NotificationTemplateResponse;
import uz.yusufjon.notificationplatform.template.dto.NotificationTemplateUpdateRequest;
import uz.yusufjon.notificationplatform.template.entity.NotificationTemplate;
import uz.yusufjon.notificationplatform.template.repository.NotificationTemplateRepository;
import uz.yusufjon.notificationplatform.notification.repository.NotificationRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateServiceTest {

    @Mock
    private NotificationTemplateRepository notificationTemplateRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationTemplateService notificationTemplateService;

    @Test
    void createShouldPersistTemplateAndDefaultActiveToTrue() {
        NotificationTemplateCreateRequest request = new NotificationTemplateCreateRequest(
                "Welcome Email",
                "Welcome",
                "Hello there",
                NotificationChannel.EMAIL,
                null
        );

        when(notificationTemplateRepository.existsByName(request.name())).thenReturn(false);
        when(notificationTemplateRepository.save(any(NotificationTemplate.class)))
                .thenAnswer(invocation -> {
                    NotificationTemplate template = invocation.getArgument(0);
                    template.setId(1L);
                    return template;
                });

        NotificationTemplateResponse response = notificationTemplateService.create(request);

        ArgumentCaptor<NotificationTemplate> templateCaptor = ArgumentCaptor.forClass(NotificationTemplate.class);
        verify(notificationTemplateRepository).save(templateCaptor.capture());

        NotificationTemplate savedTemplate = templateCaptor.getValue();
        assertThat(savedTemplate.getName()).isEqualTo(request.name());
        assertThat(savedTemplate.getSubject()).isEqualTo(request.subject());
        assertThat(savedTemplate.getBody()).isEqualTo(request.body());
        assertThat(savedTemplate.getChannel()).isEqualTo(request.channel());
        assertThat(savedTemplate.isActive()).isTrue();

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(response.active()).isTrue();
    }

    @Test
    void createShouldRejectDuplicateTemplateName() {
        NotificationTemplateCreateRequest request = new NotificationTemplateCreateRequest(
                "Welcome Email",
                "Welcome",
                "Hello there",
                NotificationChannel.EMAIL,
                true
        );

        when(notificationTemplateRepository.existsByName(request.name())).thenReturn(true);

        assertThatThrownBy(() -> notificationTemplateService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Template name already exists");
    }

    @Test
    void getByIdShouldReturnTemplateResponse() {
        NotificationTemplate template = template(5L, "Password Reset", NotificationChannel.EMAIL, true);
        template.setSubject("Reset");
        template.setBody("Reset your password");

        when(notificationTemplateRepository.findById(5L)).thenReturn(Optional.of(template));

        NotificationTemplateResponse response = notificationTemplateService.getById(5L);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.name()).isEqualTo("Password Reset");
        assertThat(response.subject()).isEqualTo("Reset");
        assertThat(response.body()).isEqualTo("Reset your password");
        assertThat(response.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(response.active()).isTrue();
    }

    @Test
    void updateShouldModifyExistingTemplate() {
        NotificationTemplate template = template(7L, "Old Name", NotificationChannel.SMS, true);
        template.setSubject("Old subject");
        template.setBody("Old body");
        NotificationTemplateUpdateRequest request = new NotificationTemplateUpdateRequest(
                "New Name",
                "New subject",
                "New body",
                NotificationChannel.PUSH,
                false
        );

        when(notificationTemplateRepository.findById(7L)).thenReturn(Optional.of(template));
        when(notificationTemplateRepository.existsByNameAndIdNot(request.name(), 7L)).thenReturn(false);
        when(notificationTemplateRepository.save(any(NotificationTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationTemplateResponse response = notificationTemplateService.update(7L, request);

        assertThat(template.getName()).isEqualTo("New Name");
        assertThat(template.getSubject()).isEqualTo("New subject");
        assertThat(template.getBody()).isEqualTo("New body");
        assertThat(template.getChannel()).isEqualTo(NotificationChannel.PUSH);
        assertThat(template.isActive()).isFalse();

        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.channel()).isEqualTo(NotificationChannel.PUSH);
        assertThat(response.active()).isFalse();
    }

    @Test
    void deleteShouldRemoveUnusedTemplate() {
        NotificationTemplate template = template(9L, "Unused", NotificationChannel.EMAIL, true);

        when(notificationTemplateRepository.findById(9L)).thenReturn(Optional.of(template));
        when(notificationRepository.existsByTemplateId(9L)).thenReturn(false);

        notificationTemplateService.delete(9L);

        verify(notificationTemplateRepository).delete(template);
    }

    @Test
    void deleteShouldRejectTemplateThatIsUsedByNotifications() {
        NotificationTemplate template = template(11L, "Used", NotificationChannel.EMAIL, true);

        when(notificationTemplateRepository.findById(11L)).thenReturn(Optional.of(template));
        when(notificationRepository.existsByTemplateId(11L)).thenReturn(true);

        assertThatThrownBy(() -> notificationTemplateService.delete(11L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Template cannot be deleted because it is used by existing notifications");

        verify(notificationTemplateRepository, never()).delete(any(NotificationTemplate.class));
    }

    private NotificationTemplate template(Long id, String name, NotificationChannel channel, boolean active) {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(id);
        template.setName(name);
        template.setChannel(channel);
        template.setActive(active);
        template.setBody("Template body");
        return template;
    }
}
