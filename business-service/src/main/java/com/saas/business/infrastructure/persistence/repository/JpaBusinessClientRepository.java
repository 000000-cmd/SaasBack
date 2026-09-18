package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.BusinessClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaBusinessClientRepository extends JpaRepository<BusinessClientEntity, UUID> {

    List<BusinessClientEntity> findByBusinessIdOrderByDisplayNameAsc(UUID businessId);

    @Query("SELECT c FROM BusinessClientEntity c "
         + "WHERE c.businessId = :businessId AND c.phoneE164 = :phone")
    Optional<BusinessClientEntity> findByBusinessAndPhone(@Param("businessId") UUID businessId,
                                                          @Param("phone") String phoneE164);

    @Query("SELECT c FROM BusinessClientEntity c "
         + "WHERE c.businessId = :businessId AND c.thirdPartyId = :thirdPartyId")
    Optional<BusinessClientEntity> findByBusinessAndThirdParty(@Param("businessId") UUID businessId,
                                                               @Param("thirdPartyId") UUID thirdPartyId);
}
