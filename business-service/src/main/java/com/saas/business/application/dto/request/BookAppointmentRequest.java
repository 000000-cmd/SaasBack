package com.saas.business.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Lo que se pide para reservar.
 *
 * <p>Llegan IDS de servicio, no precios ni duraciones: esos los resuelve el
 * servidor del catalogo y los congela en la cita. Si el cliente pudiera mandar
 * el precio, podria mandarlo a cero.</p>
 *
 * @param backdated registrar algo YA PRESTADO. Solo desde el panel o el APK, y
 *        exige el permiso {@code APPOINTMENT_BACKDATE}. Es la unica operacion
 *        que puede solaparse con lo que hubiera en la agenda.
 */
public record BookAppointmentRequest(
        @NotNull UUID businessId,
        @NotNull UUID branchId,
        @NotNull UUID employeeId,
        @NotNull UUID businessClientId,
        @NotNull Instant startUtc,
        @NotEmpty List<UUID> offeringIds,
        boolean backdated,
        @Size(max = 500) String notes
) {}
