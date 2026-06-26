package sss.thirdpartyservice.domain.port.out;

import com.saas.common.port.out.ICodeRepositoryPort;
import com.saas.common.port.out.IGenericRepositoryPort;
import sss.thirdpartyservice.domain.model.ThirdParty;

import java.util.Optional;
import java.util.UUID;

public interface IThirdPartyRepositoryPort  extends IGenericRepositoryPort<ThirdParty, UUID> {

    ThirdParty save(ThirdParty thirdParty);

    Optional<ThirdParty> findById(UUID id);

    Optional<ThirdParty> findByDocument(String documentNumber);

    boolean existsByDocument(String documentNumber);
}