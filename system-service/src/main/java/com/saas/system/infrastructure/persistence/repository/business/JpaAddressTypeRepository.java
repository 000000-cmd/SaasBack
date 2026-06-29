package com.saas.system.infrastructure.persistence.repository.business;

import com.saas.system.infrastructure.persistence.entity.business.AddressTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaAddressTypeRepository extends JpaRepository<AddressTypeEntity, UUID> {

    Optional<AddressTypeEntity> findByCode(String code);
    boolean existsByCode(String code);
}
