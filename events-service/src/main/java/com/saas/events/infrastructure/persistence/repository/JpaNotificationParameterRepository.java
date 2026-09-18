package com.saas.events.infrastructure.persistence.repository;

import com.saas.events.infrastructure.persistence.entity.NotificationParameterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaNotificationParameterRepository
        extends JpaRepository<NotificationParameterEntity, UUID> {

    Optional<NotificationParameterEntity> findByCode(String code);

    boolean existsByCode(String code);
}
