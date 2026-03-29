package uz.yusufjon.notificationplatform.template.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.yusufjon.notificationplatform.template.entity.NotificationTemplate;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findByName(String name);
}