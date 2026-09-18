package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.BusinessBookingPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaBusinessBookingPolicyRepository
        extends JpaRepository<BusinessBookingPolicyEntity, UUID> {
    Optional<BusinessBookingPolicyEntity> findByBusinessId(UUID businessId);
    Optional<BusinessBookingPolicyEntity> findByWhatsappPhoneId(String whatsappPhoneId);
    Optional<BusinessBookingPolicyEntity> findByWhatsappVerifyToken(String whatsappVerifyToken);

    @Modifying
    @Query("UPDATE BusinessBookingPolicyEntity p SET p.whatsappPhoneId = :phoneId, "
         + "p.whatsappWabaId = :wabaId, p.whatsappWebhookAt = null, "
         + "p.whatsappAccessToken = :token, p.whatsappAppSecret = :appSecret, "
         + "p.whatsappVerifyToken = :verifyToken, p.whatsappDisplayPhone = :display, "
         + "p.whatsappVerifiedName = :name, p.whatsappVerifiedAt = :verifiedAt, "
         + "p.whatsappEnabled = :enabled WHERE p.businessId = :businessId")
    int updateWhatsappCredentials(@Param("businessId") UUID businessId,
                                  @Param("phoneId") String phoneId,
                                  @Param("wabaId") String wabaId,
                                  @Param("token") String token,
                                  @Param("appSecret") String appSecret,
                                  @Param("verifyToken") String verifyToken,
                                  @Param("display") String display,
                                  @Param("name") String name,
                                  @Param("verifiedAt") java.time.LocalDateTime verifiedAt,
                                  @Param("enabled") boolean enabled);

    /**
     * Marca que el webhook quedo dado de alta contra Meta.
     *
     * <p>Va por su cuenta y no dentro de {@code applyChanges}: ese solo asigna
     * lo que no es nulo, asi que por ahi no habria forma de volver a ponerlo a
     * nulo cuando el alta deja de valer.</p>
     */
    @Modifying
    @Query("UPDATE BusinessBookingPolicyEntity p SET p.whatsappWebhookAt = :cuando "
         + "WHERE p.businessId = :businessId")
    int updateWhatsappWebhook(@Param("businessId") UUID businessId,
                              @Param("cuando") java.time.LocalDateTime cuando);
}
