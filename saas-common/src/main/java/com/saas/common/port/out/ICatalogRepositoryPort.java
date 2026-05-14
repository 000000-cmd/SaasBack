package com.saas.common.port.out;

import com.saas.common.model.BaseCatalogDomain;

/**
 * Puerto de salida para catalogos. Marcador semantico que limita el tipo a
 * subclases de {@link BaseCatalogDomain} (4 campos: code, name, value,
 * displayOrder).
 */
public interface ICatalogRepositoryPort<T extends BaseCatalogDomain, ID>
        extends ICodeRepositoryPort<T, ID> {
}
