package uz.yusufjon.notificationplatform.template.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;
import uz.yusufjon.notificationplatform.template.entity.NotificationTemplate;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    @Query("""
            select t
            from NotificationTemplate t
            where (:channel is null or t.channel = :channel)
              and (:active is null or t.active = :active)
              and (:nameKeyword = '' or lower(t.name) like lower(concat('%', :nameKeyword, '%')))
            """)
    Page<NotificationTemplate> findAllByFilters(
            @Param("channel") NotificationChannel channel,
            @Param("active") Boolean active,
            @Param("nameKeyword") String nameKeyword,
            Pageable pageable
    );
}
