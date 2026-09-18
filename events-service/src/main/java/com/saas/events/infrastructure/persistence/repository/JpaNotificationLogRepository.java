package com.saas.events.infrastructure.persistence.repository;

import com.saas.events.domain.model.SendStatus;
import com.saas.events.infrastructure.persistence.entity.NotificationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaNotificationLogRepository
        extends JpaRepository<NotificationLogEntity, UUID>, JpaSpecificationExecutor<NotificationLogEntity> {

    @Query("SELECT l.status, COUNT(l) FROM NotificationLogEntity l GROUP BY l.status")
    List<Object[]> countByStatus();

    @Query(value = "SELECT DATE(CreatedDate) AS d, " +
                   "SUM(Status = 'SENT') AS sent, SUM(Status = 'FAILED') AS failed " +
                   "FROM notification_log WHERE CreatedDate >= :from " +
                   "GROUP BY DATE(CreatedDate) ORDER BY d", nativeQuery = true)
    List<Object[]> dailySince(@Param("from") LocalDateTime from);

    long countByCreatedDateGreaterThanEqual(LocalDateTime from);

    List<NotificationLogEntity> findTop5ByStatusOrderByCreatedDateDesc(SendStatus status);

    /** Query derivada de borrado: Spring Data la ejecuta dentro de la transaccion del llamador. */
    long deleteByCreatedDateBefore(LocalDateTime cutoff);
}
