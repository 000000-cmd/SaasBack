package com.saas.events.infrastructure.persistence.repository;

import com.saas.events.infrastructure.persistence.entity.NotificationTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaNotificationTemplateRepository
        extends JpaRepository<NotificationTemplateEntity, UUID> {

    Optional<NotificationTemplateEntity> findByCode(String code);

    boolean existsByCode(String code);

    List<NotificationTemplateEntity> findByNotificationId(UUID notificationId);

    List<NotificationTemplateEntity> findByNotificationIdIsNull();
}
