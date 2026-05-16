package com.saas.system.application.service.business;

import com.saas.common.service.BaseCatalogService;
import com.saas.system.domain.model.business.ContactType;
import com.saas.system.domain.port.out.business.IContactTypeRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ContactTypeService extends BaseCatalogService<ContactType, UUID> {

    public ContactTypeService(IContactTypeRepositoryPort repository) {
        super(repository);
    }

    @Override
    public String getCatalogPath() {
        return "contact_type";
    }

    @Override
    public ContactType newInstance() {
        return new ContactType();
    }

    @Override
    protected String getResourceName() {
        return "Tipo de contacto";
    }
}
