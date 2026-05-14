package com.saas.common.port.in;

import com.saas.common.model.BaseCatalogDomain;

/**
 * Caso de uso para catalogos. Marcador semantico equivalente a
 * {@link ICodeUseCase} restringido a {@link BaseCatalogDomain}.
 */
public interface ICatalogUseCase<T extends BaseCatalogDomain, ID>
        extends ICodeUseCase<T, ID> {
}
