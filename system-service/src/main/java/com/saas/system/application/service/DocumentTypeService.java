package com.saas.system.application.service;

import com.saas.common.service.BaseCatalogService;
import com.saas.system.domain.model.DocumentType;
import com.saas.system.domain.port.out.IDocumentTypeRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DocumentTypeService extends BaseCatalogService<DocumentType, UUID> {

    public DocumentTypeService(IDocumentTypeRepositoryPort repository) {
        super(repository);
    }

    @Override
    protected String getResourceName() {
        return "Tipo de documento";
    }

    @Override
    public String getCatalogPath() {
        return "document_type";
    }

    @Override
    public DocumentType newInstance() {
        return new DocumentType();
    }
}
