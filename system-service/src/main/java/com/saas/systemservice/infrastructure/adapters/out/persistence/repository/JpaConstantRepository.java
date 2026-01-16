package com.saas.systemservice.infrastructure.adapters.out.persistence.repository;

import com.saas.systemservice.infrastructure.adapters.out.persistence.entity.ConstantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaConstantRepository extends JpaRepository<ConstantEntity, UUID> {

    Optional<ConstantEntity> findByCode(String code);

    boolean existsByCode(String code);

    @Modifying
    @Query("UPDATE ConstantEntity c SET c.enabled = :enabled WHERE c.id = :id")
    void updateEnabledStatus(UUID id, boolean enabled);
}
