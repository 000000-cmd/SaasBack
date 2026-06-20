package com.saas.common.model;

import java.util.UUID;

/**
 * Implementada por los dominios que pertenecen a un negocio (multi-tenant).
 * La auditoria la usa para sellar el {@code businessId} automaticamente.
 * Los dominios de sistema (roles, menus, ubicaciones) NO la implementan
 * y se auditan a nivel sistema (businessId = null).
 */
public interface ITenantOwned {
    UUID getBusinessId();
}
