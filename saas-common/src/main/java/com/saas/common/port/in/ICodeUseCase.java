package com.saas.common.port.in;

import com.saas.common.exception.ResourceNotFoundException;
import com.saas.common.model.BaseDomain;
import com.saas.common.model.ICodeable;

/**
 * Caso de uso para catalogos identificados por {@code Code}.
 */
public interface ICodeUseCase<T extends BaseDomain & ICodeable, ID>
        extends IGenericUseCase<T, ID> {

    /**
     * @throws ResourceNotFoundException si no existe el codigo
     */
    T getByCode(String code);
}
