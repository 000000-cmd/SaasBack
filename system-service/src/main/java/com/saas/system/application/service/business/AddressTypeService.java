package com.saas.system.application.service.business;

import com.saas.common.service.BaseCatalogService;
import com.saas.system.domain.model.business.AddressType;
import com.saas.system.domain.port.out.business.IAddressTypeRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AddressTypeService extends BaseCatalogService<AddressType, UUID> {

    public AddressTypeService(IAddressTypeRepositoryPort repository) {
        super(repository);
    }

    @Override
    public String getCatalogPath() {
        return "address_type";
    }

    @Override
    public AddressType newInstance() {
        return new AddressType();
    }

    @Override
    protected String getResourceName() {
        return "Tipo de direccion";
    }
}
