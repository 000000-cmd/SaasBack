package com.saas.system.infrastructure.persistence.repository;

import com.saas.system.infrastructure.persistence.entity.NotificationTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaNotificationTypeRepository extends JpaRepository<NotificationTypeEntity, UUID> {

    Optional<NotificationTypeEntity> findByCode(String code);

    boolean existsByCode(String code);
}
