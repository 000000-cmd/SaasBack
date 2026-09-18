package com.saas.events.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.events.domain.model.VerificationCode;
import com.saas.events.infrastructure.persistence.entity.VerificationCodeEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface VerificationCodePersistenceMapper extends IBaseMapper<VerificationCode, VerificationCodeEntity> {
}
