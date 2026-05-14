package com.saas.system.infrastructure.persistence.mapper.business;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.business.ContactType;
import com.saas.system.infrastructure.persistence.entity.business.ContactTypeEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface ContactTypePersistenceMapper extends IBaseMapper<ContactType, ContactTypeEntity> {
}
