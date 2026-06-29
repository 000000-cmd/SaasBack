package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.OfferingCategory;
import com.saas.business.infrastructure.persistence.entity.OfferingCategoryEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface OfferingCategoryPersistenceMapper extends IBaseMapper<OfferingCategory, OfferingCategoryEntity> {
}
