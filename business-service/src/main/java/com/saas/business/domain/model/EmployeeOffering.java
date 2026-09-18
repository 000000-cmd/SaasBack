package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Que servicio presta que empleado.
 *
 * <p>Sin esto no hay disponibilidad que calcular. El vinculo que habia era
 * indirecto por especialidad (empleado -> especialidad -> servicios de esa
 * especialidad), que sirve para agrupar en un menu pero no para decir "este
 * barbero hace barba pero no color". Y era una sola especialidad por persona.</p>
 *
 * <p>Los dos campos nulables son SOBREESCRITURAS: {@code null} significa
 * "hereda del servicio". El aprendiz tarda mas en el mismo corte y el maestro
 * cobra otra comision, pero la mayoria de filas no cambian nada y no tiene
 * sentido repetir el valor heredado en cada una.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class EmployeeOffering extends BaseDomain {
    private UUID employeeId;
    private UUID offeringId;
    private Integer durationMinutes;
    private BigDecimal commissionRate;
}
