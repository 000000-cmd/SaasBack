package com.saas.system.infrastructure.persistence.repository;

import com.saas.system.infrastructure.persistence.entity.DocumentTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaDocumentTypeRepository extends JpaRepository<DocumentTypeEntity, UUID> {

    Optional<DocumentTypeEntity> findByCode(String code);

    boolean existsByCode(String code);
}
