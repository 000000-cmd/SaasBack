package com.saas.thirdparty.infrastructure.persistence.repository;

import com.saas.thirdparty.infrastructure.persistence.entity.ThirdPartyContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaThirdPartyContactRepository extends JpaRepository<ThirdPartyContactEntity, UUID> {
    List<ThirdPartyContactEntity> findByThirdPartyId(UUID thirdPartyId);
}
