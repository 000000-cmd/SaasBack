package com.saas.system.infrastructure.persistence.mapper;

import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.Constant;
import com.saas.system.infrastructure.persistence.entity.ConstantEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper entre Constant (domain) y ConstantEntity (JPA).
 */
@Mapper(componentModel = "spring")
public interface ConstantPersistenceMapper extends IBaseMapper<Constant, ConstantEntity> {

    @Override
    @Mapping(target = "id", expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    Constant toDomain(ConstantEntity entity);

    @Override
    @Mapping(target = "id", expression = "java(domain.getId() != null ? java.util.UUID.fromString(domain.getId()) : null)")
    ConstantEntity toEntity(Constant domain);
}