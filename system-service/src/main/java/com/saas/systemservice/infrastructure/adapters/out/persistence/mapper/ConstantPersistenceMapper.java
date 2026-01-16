package com.saas.systemservice.infrastructure.adapters.out.persistence.mapper;


import com.saas.saascommon.infrastructure.mapper.IBaseMapper;
import com.saas.systemservice.domain.model.Constant;
import com.saas.systemservice.infrastructure.adapters.out.persistence.entity.ConstantEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConstantPersistenceMapper extends IBaseMapper<Constant, ConstantEntity> {
}