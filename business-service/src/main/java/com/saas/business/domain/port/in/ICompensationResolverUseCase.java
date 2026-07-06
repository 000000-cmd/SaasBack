package com.saas.business.domain.port.in;

import com.saas.business.domain.model.EffectiveCompensation;
import java.util.Optional;
import java.util.UUID;

/**
 * Resuelve la compensacion efectiva de un empleado escalando la jerarquia:
 * empleado -> sede -> negocio. Devuelve la primera configuracion vigente que
 * encuentra, o vacio si ningun nivel tiene una.
 */
public interface ICompensationResolverUseCase {
    Optional<EffectiveCompensation> resolveForEmployee(UUID employeeId);
}
