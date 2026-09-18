package com.saas.business.domain.availability;

import java.time.Instant;
import java.util.UUID;

/**
 * Un hueco libre, con quien lo puede atender.
 *
 * <p>Es INFORMATIVO. Que aparezca aqui no reserva nada: el bloqueo ocurre al
 * confirmar, y para entonces puede haberlo cogido otro. Cualquier pantalla que
 * asuma lo contrario acabara mostrando "reservado" sobre un hueco que ya no
 * existe.</p>
 */
public record Slot(Instant startUtc, Instant endUtc, UUID employeeId) {

    public TimeRange range() {
        return new TimeRange(startUtc, endUtc);
    }
}
