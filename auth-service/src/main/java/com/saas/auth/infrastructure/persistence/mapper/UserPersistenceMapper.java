package com.saas.auth.infrastructure.persistence.mapper;

import com.saas.auth.domain.model.User;
import com.saas.auth.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper entre User (domain) y UserEntity (JPA).
 */
@Mapper(componentModel = "spring")
public interface UserPersistenceMapper {

    @Mapping(target = "id", expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    User toDomain(UserEntity entity);

    List<User> toDomainList(List<UserEntity> entities);

    @Mapping(target = "id", expression = "java(domain.getId() != null ? java.util.UUID.fromString(domain.getId()) : null)")
    UserEntity toEntity(User domain);
}
