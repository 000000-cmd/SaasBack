package com.saas.thirdparty.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.thirdparty.domain.model.ThirdParty;
import com.saas.thirdparty.infrastructure.persistence.entity.ThirdPartyEntity;
import org.mapstruct.Mapper;

/**
 * Mapper de persistencia dominio &lt;-&gt; entidad. El CRUD (toDomain/toEntity/
 * updateEntityFromDomain) lo aporta {@link IBaseMapper}.
 */
@Mapper(config = BaseMapStructConfig.class)
public interface ThirdPartyPersistenceMapper extends IBaseMapper<ThirdParty, ThirdPartyEntity> {
}
