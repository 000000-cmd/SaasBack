package com.saas.events.infrastructure.persistence.repository;

import com.saas.events.infrastructure.persistence.entity.NotificationSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaNotificationSettingRepository extends JpaRepository<NotificationSettingEntity, UUID> {
}
