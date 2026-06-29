package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaClientRepository extends JpaRepository<ClientEntity, UUID> {
    Optional<ClientEntity> findByThirdPartyId(UUID thirdPartyId);
    boolean existsByThirdPartyId(UUID thirdPartyId);
}
