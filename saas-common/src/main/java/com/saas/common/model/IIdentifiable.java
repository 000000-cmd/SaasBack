package com.saas.common.model;

/**
 * Contrato minimo: tener un identificador. Lo cumple TODA entidad/dominio
 * del sistema (catalogos, tablas puente, etc.).
 *
 * @param <ID> tipo del identificador (en este sistema siempre UUID)
 */
public interface IIdentifiable<ID> {

    ID getId();

    void setId(ID id);
}
