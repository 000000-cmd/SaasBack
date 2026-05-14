package com.saas.system.infrastructure.persistence.repository.business;

import com.saas.system.infrastructure.persistence.entity.business.BranchTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaBranchTypeRepository extends JpaRepository<BranchTypeEntity, UUID> {

    Optional<BranchTypeEntity> findByCode(String code);
    boolean existsByCode(String code);
}
