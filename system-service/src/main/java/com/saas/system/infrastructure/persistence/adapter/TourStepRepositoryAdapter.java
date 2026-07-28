package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.TourStep;
import com.saas.system.domain.port.out.ITourStepRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.TourStepEntity;
import com.saas.system.infrastructure.persistence.mapper.TourStepPersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaTourStepRepository;
import com.saas.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public class TourStepRepositoryAdapter
        extends BaseJpaRepositoryAdapter<TourStep, TourStepEntity, UUID>
        implements ITourStepRepositoryPort {

    private final JpaTourStepRepository jpa;
    private final TourStepPersistenceMapper stepMapper;

    public TourStepRepositoryAdapter(JpaTourStepRepository jpa, TourStepPersistenceMapper mapper) {
        super(jpa, mapper, "TourStep");
        this.jpa = jpa;
        this.stepMapper = mapper;
    }

    /**
     * Override igual que en MenuRepositoryAdapter: el update base ignora las
     * referencias, asi que reasignar el menu (o quitarlo) no se guardaria.
     * Por la misma razon (NullValuePropertyMappingStrategy.IGNORE del mapper)
     * "anchor" tampoco se vaciaria si viene en null, asi que menu y anchor se
     * aplican ambos a mano.
     */
    @Override
    @Transactional
    public TourStep update(TourStep domain) {
        TourStepEntity existing = jpa.findById(domain.getId())
                .orElseThrow(() -> new ResourceNotFoundException("TourStep", "Id", domain.getId()));
        stepMapper.updateEntityFromDomain(domain, existing);
        existing.setMenu(stepMapper.toEntity(domain).getMenu());
        existing.setAnchor(domain.getAnchor());
        return stepMapper.toDomain(jpa.save(existing));
    }

    @Override
    public List<TourStep> findAllOrdered() {
        return stepMapper.toDomainList(jpa.findAllOrdered());
    }
}
