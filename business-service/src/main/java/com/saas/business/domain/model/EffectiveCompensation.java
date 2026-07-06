package com.saas.business.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Compensacion EFECTIVA de un empleado tras resolver la jerarquia
 * empleado -> sede -> negocio. No se persiste: es el resultado de la
 * resolucion. {@code source} indica de que nivel se tomo y {@code sourceId}
 * el id de la entidad (empleado, sede o negocio) que la aporto.
 */
@Getter @AllArgsConstructor @Builder
public class EffectiveCompensation {
    private final CompensationSource source;
    private final UUID sourceId;
    private final String compensationType;
    private final BigDecimal compensationValue;
    private final LocalDateTime validFrom;
    private final LocalDateTime validTo;
}
