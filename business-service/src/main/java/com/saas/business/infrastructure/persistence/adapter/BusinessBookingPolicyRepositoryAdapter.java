package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.BusinessBookingPolicy;
import com.saas.business.domain.port.out.IBusinessBookingPolicyRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.BusinessBookingPolicyEntity;
import com.saas.business.infrastructure.persistence.mapper.BusinessBookingPolicyPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaBusinessBookingPolicyRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class BusinessBookingPolicyRepositoryAdapter
        extends BaseJpaRepositoryAdapter<BusinessBookingPolicy, BusinessBookingPolicyEntity, UUID>
        implements IBusinessBookingPolicyRepositoryPort {

    private final JpaBusinessBookingPolicyRepository jpa;

    public BusinessBookingPolicyRepositoryAdapter(JpaBusinessBookingPolicyRepository jpa,
                                                  BusinessBookingPolicyPersistenceMapper mapper) {
        super(jpa, mapper, "Politica de reservas");
        this.jpa = jpa;
    }

    @Override public Optional<BusinessBookingPolicy> findByBusinessId(UUID businessId) {
        return jpa.findByBusinessId(businessId).map(getMapper()::toDomain);
    }

    @Override public Optional<BusinessBookingPolicy> findByWhatsappPhoneId(String phoneId) {
        if (phoneId == null || phoneId.isBlank()) return Optional.empty();
        return jpa.findByWhatsappPhoneId(phoneId).map(getMapper()::toDomain);
    }

    @Override public Optional<BusinessBookingPolicy> findByWhatsappVerifyToken(String verifyToken) {
        if (verifyToken == null || verifyToken.isBlank()) return Optional.empty();
        return jpa.findByWhatsappVerifyToken(verifyToken).map(getMapper()::toDomain);
    }

    /** La transaccion va AQUI: un @Modifying sin ella revienta al ejecutarse. */
    @Override
    @Transactional
    public void saveWhatsappCredentials(UUID businessId, String phoneId, String wabaId,
                                        String tokenCifrado, String appSecretCifrado,
                                        String verifyToken, String displayPhone,
                                        String verifiedName,
                                        java.time.LocalDateTime verifiedAt, boolean enabled) {
        jpa.updateWhatsappCredentials(businessId, phoneId, wabaId, tokenCifrado, appSecretCifrado,
                verifyToken, displayPhone, verifiedName, verifiedAt, enabled);
    }

    @Override
    @Transactional
    public void saveWhatsappWebhook(UUID businessId, java.time.LocalDateTime cuando) {
        jpa.updateWhatsappWebhook(businessId, cuando);
    }
}
