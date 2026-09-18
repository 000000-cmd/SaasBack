package com.saas.business.application.service;

import com.saas.business.domain.model.Appointment;
import com.saas.business.domain.model.AppointmentChannel;
import com.saas.business.domain.model.AppointmentStatus;
import com.saas.business.domain.model.BusinessBookingPolicy;
import com.saas.business.domain.port.in.IBusinessBookingPolicyUseCase;
import com.saas.business.domain.port.out.IAppointmentRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que la agenda hace sola: recordar y caducar.
 *
 * <h3>Por que los dos barridos viven juntos</h3>
 * <p>Son la misma forma —recorrer la agenda cada pocos minutos y actuar sobre
 * lo que cumple una condicion de tiempo— y comparten el mismo riesgo: que se
 * ejecuten dos veces. Separarlos en dos clases duplicaria la parte dificil sin
 * separar nada de verdad.</p>
 *
 * <h3>Seguro con dos instancias</h3>
 * <p>No hay cerrojo distribuido y no hace falta. El recordatorio deja su marca
 * con {@code INSERT IGNORE} sobre una clave unica ANTES de publicar, asi que si
 * dos instancias barren a la vez solo una publica. Y expirar es una transicion
 * de estado: la segunda encuentra la cita ya expirada y la maquina de estados
 * la rechaza.</p>
 *
 * <p>Es idempotencia por construccion, no por coordinacion — que es la unica
 * que sigue siendo cierta cuando el cerrojo falla.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgendaSweepJob {

    /**
     * Cada cuanto se barre. La ventana que se mira es igual de ancha, asi que
     * ninguna cita se queda entre dos pasadas.
     */
    private static final long CADA_MINUTOS = 5;

    private final IAppointmentRepositoryPort appointments;
    private final IBusinessBookingPolicyUseCase policies;
    private final AppointmentBookingService booking;
    private final AgendaNotifier notifier;

    @Value("${saas.agenda.sweep-enabled:true}")
    private boolean habilitado;

    /**
     * Recordatorios.
     *
     * <p>Se busca al reves de lo intuitivo: en vez de preguntar "¿a que citas
     * les toca recordatorio?", se mira la ventana {@code [ahora+H, ahora+H+5min)}
     * para cada H configurado. Asi la consulta usa el indice por fecha de
     * inicio y no hay que leer toda la agenda para descartarla.</p>
     */
    @Scheduled(fixedDelayString = "${saas.agenda.reminder-delay-ms:300000}",
               initialDelayString = "${saas.agenda.initial-delay-ms:60000}")
    public void recordar() {
        if (!habilitado) return;
        Instant ahora = Instant.now();

        // La politica se resuelve UNA vez por negocio, no por cita.
        Map<UUID, List<Integer>> horasPorNegocio = new HashMap<>();
        int enviados = 0;

        for (int horas : horasPosibles()) {
            Instant desde = ahora.plus(horas, ChronoUnit.HOURS);
            Instant hasta = desde.plus(CADA_MINUTOS, ChronoUnit.MINUTES);

            for (Appointment cita : appointments.confirmedStartingBetween(desde, hasta)) {
                List<Integer> suyas = horasPorNegocio.computeIfAbsent(
                        cita.getBusinessId(), this::horasDe);
                // Que la cita caiga en la ventana de H no significa que SU
                // negocio recuerde a H horas: cada uno configura las suyas.
                if (!suyas.contains(horas)) continue;
                if (notifier.recordatorio(cita, horas)) enviados++;
            }
        }

        if (enviados > 0) log.info("Recordatorios publicados: {}", enviados);
    }

    /**
     * Caduca lo que nadie confirmo.
     *
     * <p>Una cita pendiente ocupa agenda: mientras siga ahi, ese hueco no se le
     * puede ofrecer a nadie mas. Sin este barrido, un cliente que reserva y
     * desaparece bloquea la hora para siempre.</p>
     *
     * <p>El plazo lo pone cada negocio ({@code confirmationTimeoutMinutes}), asi
     * que se agrupa por negocio en vez de usar un corte unico.</p>
     */
    @Scheduled(fixedDelayString = "${saas.agenda.expiry-delay-ms:300000}",
               initialDelayString = "${saas.agenda.initial-delay-ms:60000}")
    public void expirar() {
        if (!habilitado) return;

        // El corte mas generoso posible; el plazo real de cada negocio se
        // comprueba fila a fila. Una sola consulta en vez de una por negocio.
        List<Appointment> candidatas = appointments.pendingOlderThan(
                LocalDateTime.now().minusMinutes(5));
        if (candidatas.isEmpty()) return;

        Map<UUID, Integer> plazoPorNegocio = new HashMap<>();
        int expiradas = 0;

        for (Appointment cita : candidatas) {
            int plazo = plazoPorNegocio.computeIfAbsent(cita.getBusinessId(), this::plazoDe);
            LocalDateTime limite = cita.getCreatedDate() == null
                    ? null : cita.getCreatedDate().plusMinutes(plazo);
            if (limite == null || limite.isAfter(LocalDateTime.now())) continue;

            try {
                booking.changeStatus(cita.getId(), AppointmentStatus.EXPIRADA,
                        AppointmentChannel.PANEL_DUENO, null,
                        "Nadie la confirmó en " + plazo + " minutos", Instant.now());
                expiradas++;
            } catch (RuntimeException ex) {
                // Otra instancia —o alguien del negocio— la movio antes. No es
                // un fallo: es justo lo que tenia que pasar.
                log.debug("La cita {} ya no estaba pendiente: {}",
                        cita.getPublicCode(), ex.getMessage());
            }
        }

        if (expiradas > 0) log.info("Citas expiradas sin confirmar: {}", expiradas);
    }

    /**
     * Todas las antelaciones que algun negocio podria tener configuradas.
     *
     * <p>Se mira una ventana por cada una y despues se filtra por negocio.
     * Recorrer los negocios primero significaria una consulta por negocio cada
     * cinco minutos, tenga citas o no.</p>
     */
    private List<Integer> horasPosibles() {
        return List.of(48, 24, 12, 6, 4, 3, 2, 1);
    }

    private List<Integer> horasDe(UUID businessId) {
        try {
            BusinessBookingPolicy p = policies.forBusiness(businessId);
            return p == null ? List.of() : p.reminderHours();
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private int plazoDe(UUID businessId) {
        try {
            BusinessBookingPolicy p = policies.forBusiness(businessId);
            Integer m = p == null ? null : p.getConfirmationTimeoutMinutes();
            return m == null || m <= 0 ? 120 : m;
        } catch (RuntimeException ex) {
            return 120;
        }
    }
}
