package com.saas.events.infrastructure.persistence.repository;

import com.saas.events.infrastructure.persistence.entity.NotificationInboxEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface JpaNotificationInboxRepository extends JpaRepository<NotificationInboxEntity, UUID> {

    Page<NotificationInboxEntity> findByThirdPartyIdOrderByCreatedDateDesc(UUID thirdPartyId, Pageable pageable);

    long countByThirdPartyIdAndReadAtIsNull(UUID thirdPartyId);

    /** Marca todas las no leidas de una persona. Una sentencia, no N. */
    @Modifying
    @Query("UPDATE NotificationInboxEntity i SET i.readAt = :now " +
           "WHERE i.thirdPartyId = :owner AND i.readAt IS NULL")
    int markAllRead(@Param("owner") UUID owner, @Param("now") LocalDateTime now);
}
