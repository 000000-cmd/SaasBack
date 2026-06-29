package com.saas.thirdparty.domain.port.out;

import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.thirdparty.domain.model.ThirdParty;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida de Terceros. Hereda el CRUD generico
 * ({@link IGenericRepositoryPort}) y agrega las busquedas por documento.
 */
public interface IThirdPartyRepositoryPort extends IGenericRepositoryPort<ThirdParty, UUID> {

    Optional<ThirdParty> findByDocument(UUID documentTypeId, String documentNumber);

    boolean existsByDocument(UUID documentTypeId, String documentNumber);
}
