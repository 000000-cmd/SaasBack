package com.saas.thirdparty.domain.port.in;

import com.saas.common.port.in.IGenericUseCase;
import com.saas.thirdparty.domain.model.BankAccount;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IBankAccountUseCase extends IGenericUseCase<BankAccount, UUID> {

    List<BankAccount> findByThirdParty(UUID thirdPartyId);

    List<BankAccount> findByThirdParties(Collection<UUID> thirdPartyIds);

    /** Deja esta como la principal y apaga la que lo fuera. */
    BankAccount makePrimary(UUID id);
}
