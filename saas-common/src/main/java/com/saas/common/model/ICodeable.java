package com.saas.common.model;

/**
 * Contrato para entidades que tienen un codigo de negocio unico
 * (ej. Role.Code = "ADMIN", Permission.Code = "EDIT", Constant.Code = "MAYORIA_EDAD").
 *
 * Las tablas puente (user_role, role_permission, menu_role) NO implementan
 * esta interfaz porque su identidad es la combinacion de FKs, no un codigo.
 */
public interface ICodeable {

    String getCode();

    void setCode(String code);
}
