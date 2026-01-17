package com.saas.system.domain.port.in;

import com.saas.common.port.in.IGenericUseCase;
import com.saas.system.domain.model.Constant;

import java.util.List;

/**
 * Puerto de entrada (caso de uso) para Constantes.
 */
public interface IConstantUseCase extends IGenericUseCase<Constant, String> {

    /**
     * Obtiene constantes por categoría
     */
    List<Constant> getByCategory(String category);
}