package com.saas.system.infrastructure.persistence.repository;

import com.saas.system.infrastructure.persistence.entity.RegistrationStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaRegistrationStatusRepository extends JpaRepository<RegistrationStatusEntity, UUID> {

    Optional<RegistrationStatusEntity> findByCode(String code);

    boolean existsByCode(String code);
}
