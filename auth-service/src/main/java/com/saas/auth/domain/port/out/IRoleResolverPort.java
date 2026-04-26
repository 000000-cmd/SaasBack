package com.saas.auth.domain.port.out;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Puerto para resolver Role.Code dado Role.Id.
 *
 * El adaptador concreto (Feign client a system-service) se cablea en Phase 7.
 * En Phase 5 puede no haber implementacion: el JWT se emite con roleCodes vacio
 * y el flujo de login sigue funcionando.
 */
public interface IRoleResolverPort {

    /** Devuelve Set vacio si Feign no esta disponible o ningun id mapea. */
    Set<String> resolveCodes(Set<UUID> roleIds);

    /** Mapeo completo Id -> Code (incluye nulls para Ids no encontrados). */
    Map<UUID, String> resolveCodeMap(Set<UUID> roleIds);
}
