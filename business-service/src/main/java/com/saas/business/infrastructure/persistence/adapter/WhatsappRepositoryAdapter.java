package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.WhatsappSession;
import com.saas.business.domain.port.out.IWhatsappRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.WhatsappSessionEntity;
import com.saas.business.infrastructure.persistence.mapper.WhatsappSessionPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaWhatsappMessageRepository;
import com.saas.business.infrastructure.persistence.repository.JpaWhatsappSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WhatsappRepositoryAdapter implements IWhatsappRepositoryPort {

    private final JpaWhatsappMessageRepository messages;
    private final JpaWhatsappSessionRepository sessions;
    private final WhatsappSessionPersistenceMapper mapper;

    /** {@code @Transactional} con el {@code @Modifying}, no en quien llama. */
    @Override
    @Transactional
    public boolean saveIfFirst(String waMessageId, UUID businessId, String fromPhone,
                               String toPhone, String body, String rawPayload) {
        return messages.insertIfAbsent(
                UUID.randomUUID().toString(), waMessageId,
                businessId == null ? null : businessId.toString(),
                fromPhone, toPhone, body, rawPayload) == 1;
    }

    @Override
    @Transactional
    public void markProcessed(String waMessageId, String error) {
        messages.markProcessed(waMessageId, error);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WhatsappSession> session(UUID businessId, String phoneE164) {
        return sessions.findByBusinessIdAndPhoneE164(businessId, phoneE164).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public WhatsappSession saveSession(WhatsappSession s) {
        WhatsappSessionEntity e = sessions
                .findByBusinessIdAndPhoneE164(s.getBusinessId(), s.getPhoneE164())
                .orElseGet(WhatsappSessionEntity::new);
        e.setBusinessId(s.getBusinessId());
        e.setPhoneE164(s.getPhoneE164());
        e.setFlowCode(s.getFlowCode());
        e.setStepCode(s.getStepCode());
        e.setDataJson(s.getDataJson());
        e.setExpiresAt(s.getExpiresAt());
        return mapper.toDomain(sessions.save(e));
    }

    @Override
    @Transactional
    public void dropSession(UUID businessId, String phoneE164) {
        sessions.findByBusinessIdAndPhoneE164(businessId, phoneE164).ifPresent(sessions::delete);
    }
}
