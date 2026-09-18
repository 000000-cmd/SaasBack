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

    /**
     * Negocio al que pertenece quien hace la peticion, SELLADO EN EL TOKEN.
     *
     * <p>Devuelve {@code null} para el administrador del sistema, que no
     * pertenece a ninguno y puede operar sobre todos.</p>
     *
     * <p>Este es el unico origen fiable del inquilino. El header
     * {@code X-Business-Id} lo pone el cliente y por tanto se puede cambiar a
     * mano; un parametro de la peticion, tambien. Antes toda consulta acotada
     * por negocio tomaba el identificador de ahi, asi que un usuario
     * autenticado podia leer los datos de otro negocio cambiando un UUID.</p>
     *
     * <p>Por defecto {@code null} para no romper implementaciones que existan
     * fuera de este modulo.</p>
     */
    default UUID getBusinessId() {
        return null;
    }
}
