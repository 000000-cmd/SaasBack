package com.saas.thirdparty.infrastructure.persistence.mapper;

import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import com.saas.thirdparty.domain.model.BankAccount;
import com.saas.thirdparty.infrastructure.persistence.entity.BankAccountEntity;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface BankAccountPersistenceMapper extends IBaseMapper<BankAccount, BankAccountEntity> {
}
