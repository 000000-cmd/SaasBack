package com.saas.thirdparty.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.thirdparty.domain.model.BankAccount;
import com.saas.thirdparty.domain.port.out.IBankAccountRepositoryPort;
import com.saas.thirdparty.infrastructure.persistence.entity.BankAccountEntity;
import com.saas.thirdparty.infrastructure.persistence.mapper.BankAccountPersistenceMapper;
import com.saas.thirdparty.infrastructure.persistence.repository.JpaBankAccountRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public class BankAccountRepositoryAdapter
        extends BaseJpaRepositoryAdapter<BankAccount, BankAccountEntity, UUID>
        implements IBankAccountRepositoryPort {

    private final JpaBankAccountRepository jpa;

    public BankAccountRepositoryAdapter(JpaBankAccountRepository jpa, BankAccountPersistenceMapper mapper) {
        super(jpa, mapper, "Cuenta bancaria");
        this.jpa = jpa;
    }

    @Override public List<BankAccount> findByThirdParty(UUID thirdPartyId) {
        return getMapper().toDomainList(
                jpa.findByThirdPartyIdOrderByIsPrimaryDescCreatedDateAsc(thirdPartyId));
    }

    @Override public List<BankAccount> findByThirdParties(Collection<UUID> thirdPartyIds) {
        if (thirdPartyIds == null || thirdPartyIds.isEmpty()) return List.of();
        return getMapper().toDomainList(jpa.findByThirdPartyIdInOrderByIsPrimaryDesc(thirdPartyIds));
    }

    @Override public long countByThirdParty(UUID thirdPartyId) {
        return jpa.countByThirdPartyId(thirdPartyId);
    }

    @Override
    @Transactional
    public void clearPrimary(UUID thirdPartyId) {
        jpa.clearPrimary(thirdPartyId);
        // El UPDATE masivo no pasa por el contexto de persistencia: sin vaciarlo,
        // el INSERT que viene detras se enviaria con la fila vieja todavia en
        // memoria y chocaria contra el UNIQUE de "una sola principal".
        jpa.flush();
    }
}
