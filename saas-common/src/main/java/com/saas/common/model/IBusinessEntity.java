package com.saas.common.model;

/**
 * Interface que deben implementar todas las entidades de negocio
 * que tienen un código único identificador.
 *
 * @param <ID> Tipo del identificador (UUID, Long, String)
 */
public interface IBusinessEntity<ID> {

    ID getId();
    void setId(ID id);

    String getCode();
    void setCode(String code);
}
