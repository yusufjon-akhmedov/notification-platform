package uz.yusufjon.notificationplatform.notification.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.yusufjon.notificationplatform.common.enums.NotificationChannel;
import uz.yusufjon.notificationplatform.common.enums.NotificationStatus;
import uz.yusufjon.notificationplatform.notification.entity.Notification;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByTemplateId(Long templateId);

    @EntityGraph(attributePaths = {"template", "createdBy"}, type = EntityGraphType.LOAD)
    @Query("""
            select n
            from Notification n
            where (:status is null or n.status = :status)
              and (:channel is null or n.channel = :channel)
              and (:createdById is null or n.createdBy.id = :createdById)
              and (:recipientKeyword is null or lower(n.recipient) like lower(concat('%', :recipientKeyword, '%')))
            """)
    Page<Notification> findAllByFilters(
            @Param("status") NotificationStatus status,
            @Param("channel") NotificationChannel channel,
            @Param("recipientKeyword") String recipientKeyword,
            @Param("createdById") Long createdById,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"template", "createdBy"})
    @Query("""
            select n
            from Notification n
            where n.id = :id
            """)
    Optional<Notification> findDetailedById(@Param("id") Long id);
}
