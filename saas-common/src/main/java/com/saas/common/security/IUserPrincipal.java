package com.saas.common.security;

import java.util.Set;
import java.util.UUID;

/**
 * Contrato comun para el principal autenticado (Authentication.getPrincipal()).
 *
 * Auth-service produce un {@code AppUserPrincipal} que implementa esta interfaz
 * a partir de la BD. El gateway, al validar el JWT, instala un principal mas
 * ligero que tambien la implementa, exponiendo el {@code userId} como UUID.
 *
 * {@link AuditorAwareImpl} consume esta interfaz para resolver el {@code AuditUser}
 * automaticamente al persistir cualquier entidad.
 */
public interface IUserPrincipal {

    UUID getUserId();

    String getUsername();

    Set<String> getRoles();
}
