package com.saas.business.application.service;

import com.saas.business.domain.model.Appointment;
import com.saas.business.domain.model.AppointmentChannel;
import com.saas.business.domain.model.AppointmentHistoryEntry;
import com.saas.business.domain.model.AppointmentLine;
import com.saas.business.domain.model.AppointmentStatus;
import com.saas.business.domain.model.CancelledBy;
import com.saas.business.domain.model.PublicCode;
import com.saas.business.domain.port.out.IAppointmentRepositoryPort;
import com.saas.common.exception.BusinessException;
import com.saas.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Crear y mover citas.
 *
 * <h3>Agendar y registrar son dos operaciones</h3>
 * <p><b>Agendar</b> (futuro) es exclusivo: una franja, una persona. Si hay
 * solape, se rechaza — y eso vale para todo el mundo, dueño incluido.</p>
 * <p><b>Registrar</b> retroactivo es anotar algo que ya se presto, y SI puede
 * solaparse con lo que hubiera: el pasado no se negocia. Solo desde el panel o
 * el APK, donde hay alguien del negocio que responde por ese registro.</p>
 * <p>Lo que el panel si puede saltarse es la antelacion minima; lo que NO puede
 * saltarse nadie es el solapamiento. Son dos validaciones distintas y estan
 * escritas por separado a proposito: una sola bandera de "es admin" que se
 * salte las dos es como se acaban agendando dos personas a la misma hora.</p>
 *
 * <h3>El orden dentro de la transaccion</h3>
 * <ol>
 *   <li>Tomar el cerrojo de {@code (empleado, dia local)}.</li>
 *   <li>Solo despues, buscar solapamientos.</li>
 *   <li>Solo despues, insertar.</li>
 * </ol>
 * <p>Invertir 1 y 2 deja la carrera abierta: las dos peticiones consultarian a
 * la vez, las dos verian el hueco libre y las dos insertarian.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentBookingService {

    /** Codigo de dominio estable: el front distingue esto de un error cualquiera. */
    public static final String SLOT_NO_DISPONIBLE = "SLOT_NO_DISPONIBLE";

    private final IAppointmentRepositoryPort repo;
    private final AgendaNotifier notifier;

    // =================================================================
    // Crear
    // =================================================================

    /**
     * Reserva la cita, o falla porque el hueco ya no esta.
     *
     * <p>{@code REPEATABLE_READ} explicito: es el nivel por defecto de MySQL,
     * pero dejarlo escrito impide que un cambio de configuracion del servidor
     * altere en silencio el comportamiento del cerrojo.</p>
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Appointment book(BookingCommand cmd) {
        validar(cmd);

        int minutos = cmd.totalMinutes();
        Instant fin = cmd.startUtc().plus(Duration.ofMinutes(minutos));
        LocalDate diaLocal = cmd.startUtc().atZone(cmd.zone()).toLocalDate();

        if (!cmd.backdated()) {
            // PRIMERO el cerrojo. Todo lo que viene despues asume que nadie mas
            // esta tocando la agenda de este empleado en este dia.
            repo.lockAgenda(cmd.employeeId(), diaLocal);

            List<Appointment> choques = repo.overlapping(cmd.employeeId(), cmd.startUtc(), fin);
            if (!choques.isEmpty()) {
                log.info("Hueco ocupado: empleado={} {}→{} choca con {} cita(s)",
                        cmd.employeeId(), cmd.startUtc(), fin, choques.size());
                throw new BusinessException(SLOT_NO_DISPONIBLE
                        + ": esa hora se acaba de ocupar. Elige otra y listo.");
            }
        }

        AppointmentStatus inicial = cmd.backdated()
                // Lo retroactivo nace COMPLETADA: se esta anotando algo que ya
                // se presto, no reservando algo por venir.
                ? AppointmentStatus.COMPLETADA
                : (cmd.requiresManualConfirmation()
                        ? AppointmentStatus.PENDIENTE_CONFIRMACION
                        : AppointmentStatus.CONFIRMADA);

        Appointment cita = repo.save(Appointment.builder()
                .businessId(cmd.businessId())
                .branchId(cmd.branchId())
                .employeeId(cmd.employeeId())
                .businessClientId(cmd.businessClientId())
                .channel(cmd.channel())
                .createdByUserId(cmd.actorUserId())
                .startUtc(cmd.startUtc())
                .endUtc(fin)
                .businessTimeZone(cmd.zone().getId())
                .localDate(diaLocal)
                .status(inicial)
                .publicCode(codigoLibre())
                .backdated(cmd.backdated())
                .rescheduledFromAppointmentId(cmd.rescheduledFromAppointmentId())
                .completedAt(cmd.backdated() ? fin : null)
                .totalPrice(cmd.totalPrice())
                .totalDurationMinutes(minutos)
                .currency("COP")
                .notes(cmd.notes())
                .build());

        int orden = 0;
        for (BookingCommand.Line l : cmd.lines()) {
            repo.saveLine(AppointmentLine.builder()
                    .appointmentId(cita.getId())
                    .offeringId(l.offeringId())
                    .serviceName(l.serviceName())
                    .price(l.price())
                    .durationMinutes(l.durationMinutes())
                    .commissionRate(l.commissionRate())
                    .commissionAmount(l.commissionAmount())
                    .displayOrder(orden++)
                    .build());
        }

        anotar(cita.getId(), null, inicial, cmd.channel(), cmd.actorUserId(),
                cmd.backdated() ? "Registro de un servicio ya prestado" : null, cmd.now());

        // Solo si nace en pie. Lo pendiente de confirmar todavia no es una cita
        // que prometer, y lo retroactivo ya paso: avisar de cualquiera de las
        // dos seria mandar un mensaje que confunde.
        if (inicial == AppointmentStatus.CONFIRMADA && !cmd.backdated()) {
            notifier.confirmada(cita);
        }

        log.info("Cita {} creada: {} {}→{} empleado={}",
                cita.getPublicCode(), inicial, cmd.startUtc(), fin, cmd.employeeId());
        return cita;
    }

    // =================================================================
    // Mover
    // =================================================================

    /**
     * Cambia el estado de una cita, comprobando que la transicion exista.
     *
     * <p>Cualquier salto que no este en la maquina de estados falla con un
     * mensaje que dice EN QUE ESTADO esta la cita: nueve de cada diez veces el
     * problema es que otra persona la movio antes.</p>
     */
    @Transactional
    public Appointment changeStatus(UUID appointmentId, AppointmentStatus destino,
                                    AppointmentChannel canal, UUID actorUserId,
                                    String motivo, Instant ahora) {
        Appointment cita = repo.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "Id", appointmentId));

        AppointmentStatus origen = cita.getStatus();
        origen.ensureCanGoTo(destino);

        if (destino == AppointmentStatus.COMPLETADA && ahora.isBefore(cita.getStartUtc())) {
            // Se puede terminar antes de la hora prevista —el corte salio
            // rapido— pero no ANTES DE EMPEZAR. Y ahi el problema no es la
            // regla: es que alguien se equivoco de cita.
            throw new BusinessException(
                    "Esta cita todavía no ha empezado: no se puede dar por terminada.");
        }

        cita.setStatus(destino);
        if (destino == AppointmentStatus.EN_CURSO)   cita.setStartedAt(ahora);
        if (destino == AppointmentStatus.COMPLETADA) cita.setCompletedAt(ahora);
        if (destino.isCancelled()) {
            cita.setCancelledAt(ahora);
            cita.setCancelReason(motivo);
            cita.setCancelledBy(quienCancela(destino));
        }

        Appointment guardada = repo.update(cita);
        anotar(appointmentId, origen, destino, canal, actorUserId, motivo, ahora);

        // El aviso va con el hecho, en la misma transaccion, PUBLICANDO al
        // outbox — no llamando a nadie. Si se enviara aqui, una cancelacion
        // dependeria de que el proveedor de mensajes conteste.
        if (destino == AppointmentStatus.CONFIRMADA) {
            // Aceptar lo que estaba pendiente ES la confirmacion del cliente.
            notifier.confirmada(guardada);
        } else if (destino.isCancelled() || destino == AppointmentStatus.NO_ASISTIO) {
            // La inasistencia tambien se avisa: el cliente tiene derecho a saber
            // que le quedo anotada, porque puede acabar bloqueandole las reservas.
            notifier.cancelada(guardada, motivo);
        }
        return guardada;
    }

    /**
     * Reprogramar: cancela la original y crea una nueva que apunta a ella.
     *
     * <p>NO es un cambio de fecha sobre la misma fila. Si lo fuera, la agenda
     * perderia que alguien movio esa cita, y esa es exactamente la pregunta que
     * aparece cuando un cliente dice "pero si a mi me habian dado las cuatro".</p>
     *
     * <p>La cita nueva pasa por {@link #book}, asi que compite por el hueco
     * como cualquier otra: reprogramar no da derecho a pisar a nadie.</p>
     */
    @Transactional
    public Appointment reschedule(UUID original, BookingCommand nueva) {
        Appointment vieja = repo.findById(original)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "Id", original));

        // La nueva PRIMERO: si el hueco nuevo esta cogido, el cliente se queda
        // con la cita que ya tenia. Cancelar antes lo dejaria sin ninguna.
        Appointment creada = book(new BookingCommand(
                nueva.businessId(), nueva.branchId(), nueva.employeeId(), nueva.businessClientId(),
                nueva.channel(), nueva.actorUserId(), nueva.startUtc(), nueva.zone(),
                nueva.lines(), false, nueva.requiresManualConfirmation(), nueva.policy(),
                nueva.now(), nueva.notes(), original));

        AppointmentStatus cierre = nueva.channel().isStaff()
                ? AppointmentStatus.CANCELADA_NEGOCIO
                : AppointmentStatus.CANCELADA_CLIENTE;
        vieja.getStatus().ensureCanGoTo(cierre);

        vieja.setStatus(cierre);
        vieja.setCancelledAt(nueva.now());
        vieja.setCancelReason("Reprogramada a " + creada.getPublicCode());
        vieja.setCancelledBy(quienCancela(cierre));
        repo.update(vieja);
        anotar(original, null, cierre, nueva.channel(), nueva.actorUserId(),
                "Reprogramada a " + creada.getPublicCode(), nueva.now());

        return creada;
    }

    // =================================================================
    // Interno
    // =================================================================

    private void validar(BookingCommand cmd) {
        if (cmd.lines() == null || cmd.lines().isEmpty()) {
            throw new BusinessException("Una cita necesita al menos un servicio");
        }
        if (cmd.totalMinutes() <= 0) {
            throw new BusinessException("Los servicios de esta cita no duran nada");
        }

        if (cmd.backdated()) {
            if (!cmd.channel().isStaff()) {
                throw new BusinessException(
                        "Solo el negocio puede registrar un servicio ya prestado");
            }
            if (cmd.startUtc().isAfter(cmd.now())) {
                throw new BusinessException(
                        "Un servicio ya prestado no puede estar en el futuro");
            }
            return;   // lo retroactivo no pasa por antelacion ni por horizonte
        }

        Instant minimo = cmd.now().plus(Duration.ofMinutes(cmd.policy().minLeadTimeMinutes()));
        // El personal del negocio SI se salta la antelacion: alguien llama y
        // dice "voy para alla". Lo que no se salta —ni el— es el solapamiento.
        if (!cmd.channel().isStaff() && cmd.startUtc().isBefore(minimo)) {
            throw new BusinessException(
                    "Esa hora ya está muy cerca. Reserva con al menos "
                            + cmd.policy().minLeadTimeMinutes() + " minutos de antelación.");
        }

        Instant tope = cmd.now().plus(Duration.ofDays(cmd.policy().maxHorizonDays()));
        if (cmd.startUtc().isAfter(tope)) {
            throw new BusinessException(
                    "Todavía no se puede reservar tan lejos: el máximo son "
                            + cmd.policy().maxHorizonDays() + " días.");
        }
    }

    private static CancelledBy quienCancela(AppointmentStatus estado) {
        return switch (estado) {
            case CANCELADA_CLIENTE -> CancelledBy.CLIENT;
            case CANCELADA_NEGOCIO -> CancelledBy.BUSINESS;
            case EXPIRADA -> CancelledBy.SYSTEM;
            default -> null;
        };
    }

    /**
     * Un codigo que no este cogido.
     *
     * <p>Con 1,1 · 10^12 combinaciones el choque es improbable, pero
     * "improbable" no es "imposible" y el indice es unico: sin este reintento,
     * una reserva fallaria con un error de clave que no explica nada.</p>
     */
    private String codigoLibre() {
        for (int intento = 0; intento < 5; intento++) {
            String codigo = PublicCode.generate();
            if (!repo.publicCodeExists(codigo)) return codigo;
        }
        throw new IllegalStateException("No se pudo generar un código público libre");
    }

    private void anotar(UUID citaId, AppointmentStatus desde, AppointmentStatus hasta,
                        AppointmentChannel canal, UUID actor, String motivo, Instant cuando) {
        repo.saveHistory(AppointmentHistoryEntry.builder()
                .appointmentId(citaId)
                .fromStatus(desde)
                .toStatus(hasta)
                .channel(canal)
                .actorUserId(actor)
                .reason(motivo)
                .occurredAt(cuando)
                .build());
    }
}
