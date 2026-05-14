package com.saas.system.domain.port.out;

import com.saas.common.port.out.ICatalogRepositoryPort;
import com.saas.system.domain.model.DocumentType;

import java.util.UUID;

public interface IDocumentTypeRepositoryPort
        extends ICatalogRepositoryPort<DocumentType, UUID> {
}
