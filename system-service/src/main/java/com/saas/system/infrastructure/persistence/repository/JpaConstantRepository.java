package com.saas.system.infrastructure.persistence.repository;

import com.saas.system.infrastructure.persistence.entity.ConstantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaConstantRepository extends JpaRepository<ConstantEntity, UUID> {

    Optional<ConstantEntity> findByCode(String code);

    boolean existsByCode(String code);
}
