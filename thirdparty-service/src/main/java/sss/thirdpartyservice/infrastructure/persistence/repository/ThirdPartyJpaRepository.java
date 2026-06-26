package sss.thirdpartyservice.infrastructure.persistence.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import sss.thirdpartyservice.infrastructure.persistence.entity.ThirdPartyEntity;

import java.util.Optional;
import java.util.UUID;

public interface ThirdPartyJpaRepository extends JpaRepository<ThirdPartyEntity, UUID> {

    Optional<ThirdPartyEntity> findByDocumentNumber(String documentNumber);

    boolean existsByDocumentNumber(String documentNumber);
}