package com.saas.system.infrastructure.persistence.mapper;

import com.saas.common.mapper.IBaseMapper;
import com.saas.system.domain.model.ListDefinition;
import com.saas.system.infrastructure.persistence.entity.ListDefinitionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper entre ListDefinition (domain) y ListDefinitionEntity (JPA).
 */
@Mapper(componentModel = "spring")
public interface ListDefinitionPersistenceMapper extends IBaseMapper<ListDefinition, ListDefinitionEntity> {

    @Override
    @Mapping(target = "id", expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    ListDefinition toDomain(ListDefinitionEntity entity);

    @Override
    @Mapping(target = "id", expression = "java(domain.getId() != null ? java.util.UUID.fromString(domain.getId()) : null)")
    ListDefinitionEntity toEntity(ListDefinition domain);
}