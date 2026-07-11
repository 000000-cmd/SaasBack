package com.saas.thirdparty.infrastructure.persistence.repository;

import com.saas.thirdparty.infrastructure.persistence.entity.ThirdPartyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaThirdPartyRepository extends JpaRepository<ThirdPartyEntity, UUID> {

    Optional<ThirdPartyEntity> findByDocumentTypeIdAndDocumentNumber(UUID documentTypeId, String documentNumber);

    boolean existsByDocumentTypeIdAndDocumentNumber(UUID documentTypeId, String documentNumber);

    /**
     * Debe existir a lo sumo un tercero por usuario. Se usa `findFirst...` (LIMIT 1)
     * para no estallar con datos heredados que tuvieran duplicados por un bug previo.
     */
    Optional<ThirdPartyEntity> findFirstByUserIdOrderByIdAsc(UUID userId);

    boolean existsByUserId(UUID userId);
}
