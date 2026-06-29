package com.saas.system.application.service.business;

import com.saas.common.service.BaseCatalogService;
import com.saas.system.domain.model.business.DayOfWeek;
import com.saas.system.domain.port.out.business.IDayOfWeekRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DayOfWeekService extends BaseCatalogService<DayOfWeek, UUID> {
    public DayOfWeekService(IDayOfWeekRepositoryPort repository) { super(repository); }
    @Override public String getCatalogPath() { return "day_of_week"; }
    @Override public DayOfWeek newInstance() { return new DayOfWeek(); }
    @Override protected String getResourceName() { return "Dia de la semana"; }
}
