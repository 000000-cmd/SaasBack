package com.saas.system.infrastructure.persistence.repository;

import com.saas.system.infrastructure.persistence.entity.SystemListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaSystemListRepository extends JpaRepository<SystemListEntity, UUID> {

    Optional<SystemListEntity> findByCode(String code);

    boolean existsByCode(String code);
}
