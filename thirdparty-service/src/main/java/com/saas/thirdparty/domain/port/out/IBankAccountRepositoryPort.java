package com.saas.thirdparty.domain.port.out;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.thirdparty.domain.model.BankAccount;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IBankAccountRepositoryPort extends IGenericRepositoryPort<BankAccount, UUID> {

    List<BankAccount> findByThirdParty(UUID thirdPartyId);

    /** En lote: nomina necesita las cuentas de todo el equipo de una vez. */
    List<BankAccount> findByThirdParties(Collection<UUID> thirdPartyIds);

    long countByThirdParty(UUID thirdPartyId);

    /** Apaga la principal actual. Debe correr ANTES de encender la nueva. */
    void clearPrimary(UUID thirdPartyId);
}
