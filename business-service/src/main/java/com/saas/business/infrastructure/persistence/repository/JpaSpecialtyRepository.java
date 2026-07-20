package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.SpecialtyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaSpecialtyRepository extends JpaRepository<SpecialtyEntity, UUID> {
    List<SpecialtyEntity> findByBusinessId(UUID businessId);
}
