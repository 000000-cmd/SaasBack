package com.saas.thirdparty.domain.port.in;

import com.saas.common.port.in.IGenericUseCase;
import com.saas.thirdparty.domain.model.ThirdParty;

import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso de Terceros. Hereda el CRUD generico y agrega busquedas por
 * documento (la identidad de negocio es la pareja documentTypeId + documentNumber).
 */
public interface IThirdPartyUseCase extends IGenericUseCase<ThirdParty, UUID> {

    Optional<ThirdParty> findByDocument(UUID documentTypeId, String documentNumber);

    boolean existsByDocument(UUID documentTypeId, String documentNumber);
}
