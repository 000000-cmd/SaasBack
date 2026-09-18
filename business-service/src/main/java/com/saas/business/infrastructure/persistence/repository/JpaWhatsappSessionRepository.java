package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.WhatsappSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaWhatsappSessionRepository extends JpaRepository<WhatsappSessionEntity, UUID> {
    Optional<WhatsappSessionEntity> findByBusinessIdAndPhoneE164(UUID businessId, String phoneE164);
}
