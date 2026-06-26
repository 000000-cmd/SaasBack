package sss.thirdpartyservice.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;
import sss.thirdpartyservice.domain.model.ThirdParty;
import sss.thirdpartyservice.infrastructure.persistence.entity.ThirdPartyEntity;

@Mapper(config = BaseMapStructConfig.class)
public interface ThirdPartyPersistenceMapper extends IBaseMapper< ThirdParty, ThirdPartyEntity  > {

    ThirdParty toDomain(ThirdPartyEntity entity);

    ThirdPartyEntity toEntity(ThirdParty domain);
}