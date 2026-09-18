package com.saas.events.infrastructure.persistence.repository;

import com.saas.events.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaNotificationRepository
        extends JpaRepository<NotificationEntity, UUID> {

    Optional<NotificationEntity> findByCode(String code);

    boolean existsByCode(String code);
}
