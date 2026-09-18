package com.saas.business.application.service;

import com.saas.business.domain.model.AgendaException;
import com.saas.business.domain.model.Appointment;
import com.saas.business.domain.port.in.IAgendaExceptionUseCase;
import com.saas.business.domain.port.out.IAgendaExceptionRepositoryPort;
import com.saas.business.domain.port.out.IAppointmentRepositoryPort;
import com.saas.common.exception.BusinessException;
import com.saas.common.service.GenericCrudService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AgendaExceptionService
        extends GenericCrudService<AgendaException, UUID>
        implements IAgendaExceptionUseCase {

    private final IAgendaExceptionRepositoryPort repo;
    private final IAppointmentRepositoryPort appointments;

    public AgendaExceptionService(IAgendaExceptionRepositoryPort repo,
                                  IAppointmentRepositoryPort appointments) {
        super(repo);
        this.repo = repo;
        this.appointments = appointments;
    }

    @Override protected String getResourceName() { return "Excepción de agenda"; }

    @Override protected void applyChanges(AgendaException e, AgendaException i) {
        if (i.getBranchId() != null) e.setBranchId(i.getBranchId());
        if (i.getEmployeeId() != null) e.setEmployeeId(i.getEmployeeId());
        if (i.getStartUtc() != null) e.setStartUtc(i.getStartUtc());
        if (i.getEndUtc() != null) e.setEndUtc(i.getEndUtc());
        if (i.getKind() != null) e.setKind(i.getKind());
        if (i.getReason() != null) e.setReason(i.getReason());
    }

    @Override
    protected void onBeforeCreate(AgendaException e) {
        validar(e);
        avisarSiPisaCitas(e);
    }

    @Override
    protected void onBeforeUpdate(AgendaException existente, AgendaException entrante) {
        validar(entrante);
    }

    private void validar(AgendaException e) {
        if (e.getStartUtc() == null || e.getEndUtc() == null) {
            throw new BusinessException("La excepción necesita inicio y fin");
        }
        if (!e.getStartUtc().isBefore(e.getEndUtc())) {
            throw new BusinessException("El fin tiene que ser posterior al inicio");
        }
        if (e.getBusinessId() == null) {
            throw new BusinessException("La excepción tiene que pertenecer a un negocio");
        }
    }

    /**
     * Bloquear tiempo que ya tiene citas vendidas no se hace en silencio.
     *
     * <p>Las citas no se cancelan solas —eso lo decide una persona, cliente a
     * cliente— asi que crear la excepcion sin avisar dejaria la agenda
     * diciendo "cerrado" mientras seis clientes siguen esperando su hora.</p>
     *
     * <p>Se puede forzar: {@code enabled = false} en la peticion significa
     * "crearla desactivada", que es como se prepara un cierre antes de haber
     * reubicado a la gente.</p>
     */
    private void avisarSiPisaCitas(AgendaException e) {
        if (Boolean.FALSE.equals(e.getEnabled())) return;

        List<Appointment> pisadas = appointments
                .busyInRange(e.getBusinessId(), e.getStartUtc(), e.getEndUtc()).stream()
                .filter(a -> e.getBranchId() == null || e.getBranchId().equals(a.getBranchId()))
                .filter(a -> e.getEmployeeId() == null || e.getEmployeeId().equals(a.getEmployeeId()))
                .toList();

        if (!pisadas.isEmpty()) {
            throw new BusinessException("Ese rango ya tiene " + pisadas.size()
                    + (pisadas.size() == 1 ? " cita agendada." : " citas agendadas.")
                    + " Muévelas o cancélalas primero, o crea el bloqueo desactivado.");
        }
    }

    @Override @Transactional(readOnly = true)
    public List<AgendaException> inRange(UUID businessId, Instant start, Instant end) {
        return repo.overlapping(businessId, start, end);
    }
}
