package sss.thirdpartyservice.domain.port.in;

import com.saas.common.port.in.ICodeUseCase;

import com.saas.common.port.in.IGenericUseCase;
import sss.thirdpartyservice.domain.model.ThirdParty;

import java.util.Optional;
import java.util.UUID;

public interface IThirdPartyUseCase extends IGenericUseCase<ThirdParty, UUID> {




    Optional<ThirdParty> findByDocument(String documentNumber);

    boolean existsDocument(String documentNumber);
}
