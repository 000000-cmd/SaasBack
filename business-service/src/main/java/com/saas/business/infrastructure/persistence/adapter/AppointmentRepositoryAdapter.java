package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.Appointment;
import com.saas.business.domain.model.AppointmentHistoryEntry;
import com.saas.business.domain.model.AppointmentLine;
import com.saas.business.domain.model.AppointmentStatus;
import com.saas.business.domain.port.out.IAppointmentRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.AppointmentEntity;
import com.saas.business.infrastructure.persistence.mapper.AppointmentHistoryPersistenceMapper;
import com.saas.business.infrastructure.persistence.mapper.AppointmentLinePersistenceMapper;
import com.saas.business.infrastructure.persistence.mapper.AppointmentPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaAgendaLockRepository;
import com.saas.business.infrastructure.persistence.repository.JpaAppointmentHistoryRepository;
import com.saas.business.infrastructure.persistence.repository.JpaAppointmentLineRepository;
import com.saas.business.infrastructure.persistence.repository.JpaAppointmentRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
public class AppointmentRepositoryAdapter
        extends BaseJpaRepositoryAdapter<Appointment, AppointmentEntity, UUID>
        implements IAppointmentRepositoryPort {

    private final JpaAppointmentRepository jpa;
    private final JpaAppointmentLineRepository lines;
    private final JpaAppointmentHistoryRepository history;
    private final JpaAgendaLockRepository locks;
    private final AgendaLockInitializer lockInitializer;
    private final AppointmentLinePersistenceMapper lineMapper;
    private final AppointmentHistoryPersistenceMapper historyMapper;

    public AppointmentRepositoryAdapter(JpaAppointmentRepository jpa,
                                        AppointmentPersistenceMapper mapper,
                                        JpaAppointmentLineRepository lines,
                                        JpaAppointmentHistoryRepository history,
                                        JpaAgendaLockRepository locks,
                                        AgendaLockInitializer lockInitializer,
                                        AppointmentLinePersistenceMapper lineMapper,
                                        AppointmentHistoryPersistenceMapper historyMapper) {
        super(jpa, mapper, "Cita");
        this.jpa = jpa;
        this.lines = lines;
        this.history = history;
        this.locks = locks;
        this.lockInitializer = lockInitializer;
        this.lineMapper = lineMapper;
        this.historyMapper = historyMapper;
    }

    @Override
    public Optional<Appointment> findByPublicCode(String publicCode) {
        return jpa.findByPublicCode(publicCode).map(getMapper()::toDomain);
    }

    @Override
    public boolean publicCodeExists(String publicCode) {
        return jpa.existsByPublicCode(publicCode);
    }

    /**
     * Toma el cerrojo del dia.
     *
     * <p>Dos pasos, y el orden importa. Primero se garantiza que la fila EXISTA
     * Y ESTE CONFIRMADA, en una transaccion aparte (ver
     * {@link AgendaLockInitializer}: crearla dentro de esta provocaba un
     * deadlock entre dos reservas simultaneas). Solo despues se toma el
     * bloqueo, que ya es un bloqueo de fila normal de InnoDB.</p>
     */
    @Override
    public void lockAgenda(UUID employeeId, LocalDate localDate) {
        lockInitializer.ensureExists(employeeId, localDate);
        locks.lock(employeeId, localDate).orElseThrow(() -> new IllegalStateException(
                "No se pudo tomar el cerrojo de la agenda de " + employeeId + " el " + localDate));
    }

    @Override
    public List<Appointment> overlapping(UUID employeeId, Instant start, Instant end) {
        return getMapper().toDomainList(
                jpa.overlapping(employeeId, start, end, AppointmentStatus.occupying()));
    }

    @Override
    public List<Appointment> busyInRange(UUID businessId, Instant start, Instant end) {
        return getMapper().toDomainList(
                jpa.busyInRange(businessId, start, end, AppointmentStatus.occupying()));
    }

    @Override
    public List<Appointment> byBusinessAndDay(UUID businessId, LocalDate day) {
        return getMapper().toDomainList(jpa.byBusinessAndDay(businessId, day));
    }

    @Override
    public List<Appointment> byEmployeeBetween(UUID employeeId, LocalDate from, LocalDate to) {
        return getMapper().toDomainList(jpa.byEmployeeBetween(employeeId, from, to));
    }

    @Override
    public List<Appointment> pendingOlderThan(LocalDateTime cutoff) {
        return getMapper().toDomainList(jpa.pendingOlderThan(cutoff));
    }

    @Override
    public List<Appointment> confirmedStartingBetween(java.time.Instant from, java.time.Instant to) {
        return getMapper().toDomainList(jpa.confirmedStartingBetween(from, to));
    }

    @Override
    public List<AppointmentLine> linesOf(UUID appointmentId) {
        return lineMapper.toDomainList(lines.findByAppointmentIdOrderByDisplayOrderAsc(appointmentId));
    }

    @Override
    public List<AppointmentLine> linesOfMany(java.util.Collection<UUID> appointmentIds) {
        if (appointmentIds == null || appointmentIds.isEmpty()) return List.of();
        return lineMapper.toDomainList(
                lines.findByAppointmentIdInOrderByDisplayOrderAsc(appointmentIds));
    }

    @Override
    public AppointmentLine saveLine(AppointmentLine line) {
        return lineMapper.toDomain(lines.save(lineMapper.toEntity(line)));
    }

    @Override
    public AppointmentHistoryEntry saveHistory(AppointmentHistoryEntry entry) {
        return historyMapper.toDomain(history.save(historyMapper.toEntity(entry)));
    }

    @Override
    public List<AppointmentHistoryEntry> historyOf(UUID appointmentId) {
        return historyMapper.toDomainList(history.findByAppointmentIdOrderByOccurredAtAsc(appointmentId));
    }
}
