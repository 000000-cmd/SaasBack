package com.saas.thirdparty.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.thirdparty.domain.model.ThirdParty;
import com.saas.thirdparty.domain.port.out.IThirdPartyRepositoryPort;
import com.saas.thirdparty.infrastructure.persistence.entity.ThirdPartyEntity;
import com.saas.thirdparty.infrastructure.persistence.mapper.ThirdPartyPersistenceMapper;
import com.saas.thirdparty.infrastructure.persistence.repository.JpaThirdPartyRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter JPA de Terceros. Hereda el CRUD generico de
 * {@link BaseJpaRepositoryAdapter} y solo implementa las busquedas por documento.
 */
@Repository
public class ThirdPartyRepositoryAdapter
        extends BaseJpaRepositoryAdapter<ThirdParty, ThirdPartyEntity, UUID>
        implements IThirdPartyRepositoryPort {

    private final JpaThirdPartyRepository jpa;

    public ThirdPartyRepositoryAdapter(JpaThirdPartyRepository jpa, ThirdPartyPersistenceMapper mapper) {
        super(jpa, mapper, "Tercero");
        this.jpa = jpa;
    }

    @Override
    public Optional<ThirdParty> findByDocument(UUID documentTypeId, String documentNumber) {
        return jpa.findByDocumentTypeIdAndDocumentNumber(documentTypeId, documentNumber)
                .map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByDocument(UUID documentTypeId, String documentNumber) {
        return jpa.existsByDocumentTypeIdAndDocumentNumber(documentTypeId, documentNumber);
    }
}
