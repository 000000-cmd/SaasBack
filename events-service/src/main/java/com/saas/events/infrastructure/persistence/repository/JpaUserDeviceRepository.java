package com.saas.events.infrastructure.persistence.repository;

import com.saas.events.infrastructure.persistence.entity.UserDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaUserDeviceRepository extends JpaRepository<UserDeviceEntity, UUID> {
    Optional<UserDeviceEntity> findByFcmToken(String fcmToken);
    List<UserDeviceEntity> findByThirdPartyId(UUID thirdPartyId);
    void deleteByFcmToken(String fcmToken);
}
