package com.saas.business.domain.port.out;

import com.saas.business.domain.model.Appointment;
import com.saas.business.domain.model.AppointmentHistoryEntry;
import com.saas.business.domain.model.AppointmentLine;
import com.saas.common.port.out.IGenericRepositoryPort;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IAppointmentRepositoryPort extends IGenericRepositoryPort<Appointment, UUID> {

    Optional<Appointment> findByPublicCode(String publicCode);

    boolean publicCodeExists(String publicCode);

    /**
     * Toma el cerrojo de la agenda de ese empleado ese dia.
     *
     * <p>SE LLAMA PRIMERO, antes de comprobar nada. Es lo que serializa dos
     * reservas simultaneas al mismo hueco. Si la fila no existe todavia, se
     * crea y se vuelve a intentar.</p>
     */
    void lockAgenda(UUID employeeId, LocalDate localDate);

    /**
     * Citas del empleado que se solapan con el rango y ocupan agenda.
     *
     * <p>Excluye las retroactivas: registrar algo ya prestado puede convivir
     * con lo que hubiera.</p>
     */
    List<Appointment> overlapping(UUID employeeId, Instant start, Instant end);

    /** Lo que ocupa agenda en el negocio dentro de un rango. Alimenta el motor. */
    List<Appointment> busyInRange(UUID businessId, Instant start, Instant end);

    List<Appointment> byBusinessAndDay(UUID businessId, LocalDate day);

    List<Appointment> byEmployeeBetween(UUID employeeId, LocalDate from, LocalDate to);

    /** Pendientes de confirmar que ya se pasaron de plazo. */
    List<Appointment> pendingOlderThan(java.time.LocalDateTime cutoff);

    /** Citas en pie que empiezan dentro de un rango. Lo barre el recordatorio. */
    List<Appointment> confirmedStartingBetween(Instant from, Instant to);

    // ---- Lineas de servicio ----
    List<AppointmentLine> linesOf(UUID appointmentId);

    /** Las lineas de varias citas de una vez. Evita un viaje por cita. */
    List<AppointmentLine> linesOfMany(java.util.Collection<UUID> appointmentIds);

    AppointmentLine saveLine(AppointmentLine line);

    // ---- Historial ----
    AppointmentHistoryEntry saveHistory(AppointmentHistoryEntry entry);

    List<AppointmentHistoryEntry> historyOf(UUID appointmentId);
}
