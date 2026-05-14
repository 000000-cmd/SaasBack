package com.saas.system.infrastructure.persistence.repository.business;

import com.saas.system.infrastructure.persistence.entity.business.ContactTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaContactTypeRepository extends JpaRepository<ContactTypeEntity, UUID> {

    Optional<ContactTypeEntity> findByCode(String code);
    boolean existsByCode(String code);

}
