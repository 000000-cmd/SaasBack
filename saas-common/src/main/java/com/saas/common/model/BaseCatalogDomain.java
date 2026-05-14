package com.saas.common.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Modelo de dominio base para catalogos. Espejo de
 * {@link com.saas.common.persistence.BaseCatalogEntity}.
 *
 * Cada catalogo (tipos de documento, estados, generos, etc.) extiende esta
 * clase y vive en su propia tabla. Aunque la estructura es identica, cada
 * catalogo es semanticamente independiente.
 */
@Getter
@Setter
public abstract class BaseCatalogDomain extends BaseDomain implements ICodeable {

    private String code;
    private String name;
    private String value;
    private Integer displayOrder = 0;
}
