package com.saas.thirdparty.infrastructure.persistence.repository;

import com.saas.thirdparty.infrastructure.persistence.entity.ThirdPartyAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaThirdPartyAddressRepository extends JpaRepository<ThirdPartyAddressEntity, UUID> {
    List<ThirdPartyAddressEntity> findByThirdPartyId(UUID thirdPartyId);
}
