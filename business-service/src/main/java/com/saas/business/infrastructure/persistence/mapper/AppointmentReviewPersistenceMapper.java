package com.saas.business.infrastructure.persistence.mapper;

import com.saas.business.domain.model.AppointmentReview;
import com.saas.business.infrastructure.persistence.entity.AppointmentReviewEntity;
import com.saas.common.mapper.BaseMapStructConfig;
import com.saas.common.mapper.IBaseMapper;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapStructConfig.class)
public interface AppointmentReviewPersistenceMapper extends IBaseMapper<AppointmentReview, AppointmentReviewEntity> {
}
