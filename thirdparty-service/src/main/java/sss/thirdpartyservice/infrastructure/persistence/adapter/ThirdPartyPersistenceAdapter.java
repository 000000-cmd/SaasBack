package sss.thirdpartyservice.infrastructure.persistence.adapter;


import com.saas.common.persistence.BaseJpaRepositoryAdapter;

import com.saas.system.domain.model.Menu;
import org.springframework.stereotype.Repository;

import sss.thirdpartyservice.domain.model.ThirdParty;
import sss.thirdpartyservice.domain.port.out.IThirdPartyRepositoryPort;
import sss.thirdpartyservice.infrastructure.persistence.entity.ThirdPartyEntity;
import sss.thirdpartyservice.infrastructure.persistence.mapper.ThirdPartyPersistenceMapper;
import sss.thirdpartyservice.infrastructure.persistence.repository.ThirdPartyJpaRepository;


import java.util.Optional;

import java.util.UUID;

@Repository
public class ThirdPartyPersistenceAdapter
extends BaseJpaRepositoryAdapter<ThirdParty, ThirdPartyEntity, UUID>
        implements IThirdPartyRepositoryPort {

    private final ThirdPartyJpaRepository jpa;
    private final ThirdPartyPersistenceMapper menuMapper;

    public ThirdPartyPersistenceAdapter (ThirdPartyJpaRepository jpa, ThirdPartyPersistenceMapper mapper) {
        super(jpa, mapper, "Thirdparty");
        this.jpa = jpa;
        this.menuMapper = mapper;
    }

    @Override
    public ThirdParty save(ThirdParty thirdParty) {
        return menuMapper.toDomain(
                jpa.save(
                        menuMapper.toEntity(thirdParty)
                )
        );
    }


    @Override
    public Optional<ThirdParty> findById(UUID id) {
        return jpa.findById(id)
                .map(menuMapper::toDomain);
    }

    @Override
    public Optional<ThirdParty> findByDocument(String documentNumber) {
        return jpa.findByDocumentNumber(documentNumber)
                .map(menuMapper::toDomain);
    }

    @Override
    public boolean existsByDocument(String documentNumber) {
        return jpa.existsByDocumentNumber(documentNumber);
    }
}
