package com.saas.finance.domain.port.in;

import com.saas.finance.domain.model.EffectiveCompensation;
import java.util.Optional;
import java.util.UUID;

/**
 * Resuelve la compensacion efectiva escalando la jerarquia empleado -> sede
 * -> negocio, devolviendo la primera configuracion vigente que encuentra.
 *
 * <p>Finance NO posee las tablas de empleado/sede/negocio (viven en
 * business-service). Por eso el caller provee la cadena de IDs: en el flujo
 * real llegan dentro del evento de la cita (employeeId, branchId, businessId).
 * {@code branchId} y {@code businessId} son opcionales: si son {@code null}
 * ese nivel se omite en la escalada.</p>
 */
public interface ICompensationResolverUseCase {
    Optional<EffectiveCompensation> resolveForEmployee(UUID employeeId, UUID branchId, UUID businessId);
}
