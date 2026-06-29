package com.saas.business.application.service;

import com.saas.business.domain.model.Business;
import com.saas.business.domain.port.in.IBusinessUseCase;
import com.saas.business.domain.port.out.IBusinessRepositoryPort;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BusinessService extends GenericCrudService<Business, UUID> implements IBusinessUseCase {

    public BusinessService(IBusinessRepositoryPort repo) { super(repo); }

    @Override protected String getResourceName() { return "Empresa"; }

    @Override
    protected void applyChanges(Business existing, Business incoming) {
        if (incoming.getBusinessTypeId() != null) existing.setBusinessTypeId(incoming.getBusinessTypeId());
        if (incoming.getName() != null)           existing.setName(incoming.getName());
        if (incoming.getLegalName() != null)      existing.setLegalName(incoming.getLegalName());
        if (incoming.getTradeName() != null)      existing.setTradeName(incoming.getTradeName());
        if (incoming.getDocumentTypeId() != null) existing.setDocumentTypeId(incoming.getDocumentTypeId());
        if (incoming.getDocumentNumber() != null) existing.setDocumentNumber(incoming.getDocumentNumber());
        if (incoming.getLogoUrl() != null)        existing.setLogoUrl(incoming.getLogoUrl());
        if (incoming.getStatusId() != null)       existing.setStatusId(incoming.getStatusId());
        if (incoming.getPrimaryColor() != null)   existing.setPrimaryColor(incoming.getPrimaryColor());
        if (incoming.getSecondaryColor() != null) existing.setSecondaryColor(incoming.getSecondaryColor());
    }
}
