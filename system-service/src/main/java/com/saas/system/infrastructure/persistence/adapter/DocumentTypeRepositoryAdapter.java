package com.saas.system.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.system.domain.model.DocumentType;
import com.saas.system.domain.port.out.IDocumentTypeRepositoryPort;
import com.saas.system.infrastructure.persistence.entity.DocumentTypeEntity;
import com.saas.system.infrastructure.persistence.mapper.DocumentTypePersistenceMapper;
import com.saas.system.infrastructure.persistence.repository.JpaDocumentTypeRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class DocumentTypeRepositoryAdapter
        extends BaseJpaRepositoryAdapter<DocumentType, DocumentTypeEntity, UUID>
        implements IDocumentTypeRepositoryPort {

    private final JpaDocumentTypeRepository jpa;

    public DocumentTypeRepositoryAdapter(JpaDocumentTypeRepository jpa,
                                         DocumentTypePersistenceMapper mapper) {
        super(jpa, mapper, "Tipo de documento");
        this.jpa = jpa;
    }

    @Override
    public Optional<DocumentType> findByCode(String code) {
        return jpa.findByCode(code).map(getMapper()::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }
}
