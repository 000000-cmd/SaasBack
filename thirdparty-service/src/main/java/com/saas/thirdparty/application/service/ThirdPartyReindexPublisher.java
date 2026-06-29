package com.saas.thirdparty.application.service;

import com.saas.common.events.EventTypes;
import com.saas.common.outbox.OutboxPublisher;
import com.saas.thirdparty.application.dto.event.ThirdPartyEventPayload;
import com.saas.thirdparty.domain.model.ThirdParty;
import com.saas.thirdparty.domain.port.out.IThirdPartyRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Publica el evento de tercero para que search-service indexe el read model de
 * LISTADO/BUSQUEDA. El payload es plano (documento, nombre, estado, Ids de filtro);
 * el detalle (contactos/direcciones/catalogos) no se indexa. Lo usan el servicio de
 * terceros y los de contactos/direcciones para refrescar el documento del padre.
 */
@Component
@RequiredArgsConstructor
public class ThirdPartyReindexPublisher {

    private static final String AGGREGATE_TYPE = "thirdparty";

    private final IThirdPartyRepositoryPort thirdPartyRepo;
    private final OutboxPublisher outboxPublisher;

    /** Publica el tercero. {@code created=true} en alta. */
    public void publishUpsert(UUID thirdPartyId, boolean created) {
        thirdPartyRepo.findById(thirdPartyId).ifPresent(tp ->
                outboxPublisher.publish(
                        created ? EventTypes.THIRDPARTY_CREATED : EventTypes.THIRDPARTY_UPDATED,
                        null, AGGREGATE_TYPE, thirdPartyId, ThirdPartyEventPayload.from(tp)));
    }

    /** Re-indexa el tercero tras un cambio en un hijo (contacto/direccion). */
    public void reindex(UUID thirdPartyId) {
        publishUpsert(thirdPartyId, false);
    }

    public void publishDelete(UUID thirdPartyId, ThirdParty snapshot) {
        outboxPublisher.publish(EventTypes.THIRDPARTY_DELETED, null, AGGREGATE_TYPE,
                thirdPartyId, ThirdPartyEventPayload.from(snapshot));
    }

    /** Construye los payloads de una página (para reindex-from-source). */
    public List<ThirdPartyEventPayload> buildPage(int page, int size) {
        return thirdPartyRepo.findAllPaged(page, size).stream().map(ThirdPartyEventPayload::from).toList();
    }
}
