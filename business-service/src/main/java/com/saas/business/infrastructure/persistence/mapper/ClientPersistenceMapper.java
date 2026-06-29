package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.Client;
import com.saas.business.infrastructure.persistence.entity.ClientEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface ClientPersistenceMapper extends IBaseMapper<Client, ClientEntity> {
}
