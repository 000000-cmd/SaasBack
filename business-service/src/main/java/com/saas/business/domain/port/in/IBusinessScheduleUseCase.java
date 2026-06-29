package com.saas.business.domain.port.in;

import com.saas.business.domain.model.BusinessSchedule;
import com.saas.common.port.in.IGenericUseCase;
import java.util.List;
import java.util.UUID;

public interface IBusinessScheduleUseCase extends IGenericUseCase<BusinessSchedule, UUID> {
    List<BusinessSchedule> findByBusiness(UUID businessId);
    List<BusinessSchedule> findCurrentByBusiness(UUID businessId);
    /** Cierra la version vigente y crea una nueva (trazabilidad). */
    BusinessSchedule supersede(UUID currentId, BusinessSchedule incoming);
}
