package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Un servicio dentro de una cita, CON SU FOTO DEL MOMENTO.
 *
 * <p>Una cita puede llevar varios (corte + barba), asi que el snapshot vive
 * aqui y la cita guarda solo los totales.</p>
 *
 * <p>Los importes se congelan y esto NO es duplicar datos: si el dueño sube el
 * precio del corte en julio, las citas de marzo no pueden cambiar de valor,
 * porque de ahi salieron comisiones que ya se pagaron. El {@code offeringId} se
 * mantiene para poder trazar de que servicio del catalogo vino, pero el numero
 * que manda es este.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AppointmentLine extends BaseDomain {

    private UUID appointmentId;
    /** Puede quedar apuntando a un servicio deshabilitado: por eso el nombre tambien se congela. */
    private UUID offeringId;

    private String serviceName;
    private BigDecimal price;
    private Integer durationMinutes;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private Integer displayOrder;
}
