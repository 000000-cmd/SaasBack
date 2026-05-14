package com.saas.system.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.DocumentType;
import com.saas.system.infrastructure.persistence.entity.DocumentTypeEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface DocumentTypePersistenceMapper
        extends IBaseMapper<DocumentType, DocumentTypeEntity> {
}
