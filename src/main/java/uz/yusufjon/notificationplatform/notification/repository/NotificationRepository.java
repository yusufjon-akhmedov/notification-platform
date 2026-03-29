package uz.yusufjon.notificationplatform.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;
import uz.yusufjon.notificationplatform.common.enums.NotificationStatus;
import uz.yusufjon.notificationplatform.notification.entity.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByStatus(NotificationStatus status);

    boolean existsByTemplateId(Long templateId);

    @Query("""
            select n
            from Notification n
            where (:status is null or n.status = :status)
              and (:channel is null or n.channel = :channel)
              and (:createdById is null or n.createdBy.id = :createdById)
            order by n.createdAt desc
            """)
    List<Notification> findAllByFilters(
            @Param("status") NotificationStatus status,
            @Param("channel") NotificationChannel channel,
            @Param("createdById") Long createdById
    );

    @Query("""
            select n
            from Notification n
            where n.id = :id
            """)
    Optional<Notification> findDetailedById(@Param("id") Long id);
}
