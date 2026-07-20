package com.saas.business.application.service;

import com.saas.business.infrastructure.client.SearchClient;
import com.saas.business.infrastructure.client.ThirdPartyClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Resuelve el tercero (persona) de una cuenta para LECTURAS.
 *
 * <p>Va primero al read model de Elasticsearch y solo cae a thirdparty-service
 * si ES no lo tiene proyectado. Un unico punto para que ambos consumidores
 * (mi-negocio y owner-business) compartan la misma politica de respaldo.</p>
 *
 * <p><b>No usar en orquestaciones de escritura.</b> El aprovisionamiento decide
 * crear-vs-actualizar persona con esta resolucion: leerla de ES (eventualmente
 * consistente) podria duplicar la persona. Ese flujo se queda contra la fuente
 * de verdad a proposito.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonLookupService {

    private final SearchClient searchClient;
    private final ThirdPartyClient thirdPartyClient;

    /** Id del tercero vinculado a la cuenta, o vacio si no tiene persona. */
    public Optional<UUID> thirdPartyIdByUser(UUID userId) {
        try {
            SearchClient.PersonDoc doc = searchClient.personByUser(userId);
            if (doc != null && doc.id() != null) return Optional.of(doc.id());
        } catch (Exception ex) {
            log.debug("ES no resolvio la persona de userId={}, respaldo a thirdparty: {}", userId, ex.getMessage());
        }
        try {
            return Optional.ofNullable(thirdPartyClient.personByUser(userId)).map(ThirdPartyClient.PersonResponse::id);
        } catch (Exception ex) {
            // Sin persona vinculada (aun no aprovisiono): no es error.
            log.debug("Sin persona para userId={}: {}", userId, ex.getMessage());
            return Optional.empty();
        }
    }
}
