package com.saas.system.application.service.business;

import com.saas.common.service.BaseCatalogService;
import com.saas.system.domain.model.business.ShiftType;
import com.saas.system.domain.port.out.business.IShiftTypeRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ShiftTypeService extends BaseCatalogService<ShiftType, UUID> {

    public ShiftTypeService(IShiftTypeRepositoryPort repository) {
        super(repository);
    }

    @Override
    public String getCatalogPath() {
        return "shift_type";
    }

    @Override
    public ShiftType newInstance() {
        return new ShiftType();
    }

    @Override
    protected String getResourceName() {
        return "Tipo de turno";
    }
}
