package com.saas.finance.application.service;

import com.saas.finance.domain.model.ChargeStatus;
import com.saas.finance.domain.model.EffectiveCompensation;
import com.saas.finance.domain.model.PaymentMethod;
import com.saas.finance.domain.model.ServiceCharge;
import com.saas.finance.domain.port.in.ICompensationResolverUseCase;
import com.saas.finance.domain.port.out.IServiceChargeRepositoryPort;
import com.saas.finance.infrastructure.client.BusinessAgendaClient;
import com.saas.finance.infrastructure.client.BusinessAgendaClient.AppointmentForCharge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * El eslabon que faltaba: convierte lo que se agenda en lo que se cobra.
 *
 * <p>Hasta ahora la cadena cargo → liquidacion → saldo → nomina estaba montada
 * y probada de la mitad para adelante, y no tenia entrada: nada creaba nunca un
 * {@code service_charge}, asi que ninguna comision llegaba a ningun saldo.</p>
 *
 * <h3>Como funciona</h3>
 * <p>Pregunta a la agenda por las citas de una ventana y, para cada una, crea
 * el cargo que le falte:</p>
 * <ul>
 *   <li>cita futura o en curso → cargo {@code SCHEDULED}: existe, pero todavia
 *       no hay nada que aprobar;</li>
 *   <li>cita {@code COMPLETADA} → cargo {@code PENDING}: el servicio se presto
 *       y espera la decision del dueno;</li>
 *   <li>cancelada o inasistencia → ningun cargo, y si ya existia uno programado
 *       se descarta con su motivo. Un servicio que no se presto no se cobra.</li>
 * </ul>
 *
 * <h3>Por que un sincronizador y no una llamada al completar</h3>
 * <p>Llamar a finance desde la transaccion de la agenda mete una llamada de red
 * donde no cabe: si finance no responde, o la cita no se completa o el cargo no
 * se crea. Aqui, si una pasada falla, la siguiente recoge lo que quedo; si
 * finance estuvo caido un dia, al volver se pone al dia solo.</p>
 *
 * <h3>Idempotencia</h3>
 * <p>Doble: se consulta que cargos ya existen antes de crear, y el indice unico
 * {@code uq_sc_appointment} respalda por si dos pasadas se cruzan. Correrlo dos
 * veces seguidas no le paga nada de mas a nadie.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentChargeSyncService {

    /** Estados de la cita que ya no van a prestarse. */
    private static final Set<String> MUERTAS = Set.of(
            "CANCELADA_CLIENTE", "CANCELADA_NEGOCIO", "NO_ASISTIO", "EXPIRADA");

    private final BusinessAgendaClient agenda;
    private final IServiceChargeRepositoryPort charges;
    private final ICompensationResolverUseCase compensations;

    /** Lo que hizo una pasada. Se devuelve para poder afirmarlo en una prueba. */
    public record Result(int creados, int actualizados, int descartados, int omitidos) {}

    @Transactional
    public Result sync(UUID businessId, LocalDate from, LocalDate to) {
        List<AppointmentForCharge> citas =
                agenda.forCharges(businessId, from.toString(), to.toString());
        if (citas.isEmpty()) return new Result(0, 0, 0, 0);

        Map<UUID, ServiceCharge> existentes = new HashMap<>();
        for (ServiceCharge c : charges.findByAppointments(
                citas.stream().map(AppointmentForCharge::appointmentId).toList())) {
            existentes.put(c.getAppointmentId(), c);
        }

        // La compensacion se resuelve UNA vez por empleado, no por cita: es la
        // misma para todas las de esa pasada y resolverla por fila serian tres
        // consultas por cita para el mismo numero.
        Map<UUID, BigDecimal> porcentajePorEmpleado = new HashMap<>();
        int creados = 0, actualizados = 0, descartados = 0, omitidos = 0;

        for (AppointmentForCharge cita : citas) {
            ServiceCharge existente = existentes.get(cita.appointmentId());

            if (MUERTAS.contains(cita.status())) {
                if (existente != null && existente.getStatus() == ChargeStatus.SCHEDULED) {
                    existente.setStatus(ChargeStatus.DISCARDED);
                    existente.setDiscardedAt(java.time.LocalDateTime.now());
                    existente.setDiscardReason("La cita quedó en " + legible(cita.status()));
                    charges.update(existente);
                    descartados++;
                } else {
                    omitidos++;
                }
                continue;
            }

            ChargeStatus destino = "COMPLETADA".equals(cita.status())
                    ? ChargeStatus.PENDING
                    : ChargeStatus.SCHEDULED;

            if (existente != null) {
                // Solo se adelanta el estado, nunca se retrocede: un cargo que
                // el dueno ya aprobo o descarto no lo toca el sincronizador.
                if (existente.getStatus() == ChargeStatus.SCHEDULED
                        && destino == ChargeStatus.PENDING) {
                    existente.setStatus(ChargeStatus.PENDING);
                    charges.update(existente);
                    actualizados++;
                } else {
                    omitidos++;
                }
                continue;
            }

            BigDecimal porcentaje = porcentajePorEmpleado.computeIfAbsent(
                    cita.employeeId(),
                    e -> porcentajeDe(e, cita.branchId(), cita.businessId()));

            charges.save(nuevoCargo(cita, destino, porcentaje));
            creados++;
        }

        if (creados + actualizados + descartados > 0) {
            log.info("Cargos sincronizados negocio={} {}..{}: {} creados, {} al día, {} descartados",
                    businessId, from, to, creados, actualizados, descartados);
        }
        return new Result(creados, actualizados, descartados, omitidos);
    }

    /**
     * El cargo, con los importes CONGELADOS.
     *
     * <p>El bruto sale del snapshot de la cita —lo que se cobro cuando se
     * agendo— y no del catalogo de hoy. Si el dueno sube el precio en julio, la
     * cita de marzo sigue valiendo lo de marzo, que es de donde salio la
     * comision que ya se liquido.</p>
     */
    private ServiceCharge nuevoCargo(AppointmentForCharge cita, ChargeStatus estado,
                                     BigDecimal porcentajeEmpleado) {
        BigDecimal bruto = cita.totalPrice() == null ? BigDecimal.ZERO : cita.totalPrice();
        BigDecimal neto = bruto.multiply(porcentajeEmpleado)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal retenido = bruto.subtract(neto);

        return ServiceCharge.builder()
                .businessId(cita.businessId())
                .branchId(cita.branchId())
                .employeeId(cita.employeeId())
                .appointmentId(cita.appointmentId())
                .serviceName(cita.serviceName())
                .serviceDate(cita.serviceDate())
                .startTime(cita.startTime())
                .endTime(cita.endTime())
                .clientName(cita.clientName())
                .clientPhone(cita.clientPhone())
                .clientThirdPartyId(cita.clientThirdPartyId())
                .grossAmount(bruto)
                .deductionRate(BigDecimal.valueOf(100).subtract(porcentajeEmpleado))
                .deductionAmount(retenido)
                .netAmount(neto)
                .currency("COP")
                // El medio de pago lo declara quien cobra, al terminar el
                // servicio. Nace en efectivo porque es lo que no exige
                // comprobante: suponer transferencia bloquearia la aprobacion
                // de todos los cargos con un soporte que nadie prometio.
                .paymentMethod(PaymentMethod.CASH)
                .status(estado)
                .build();
    }

    /**
     * Que porcentaje del servicio se lleva el empleado.
     *
     * <p>Sale de la compensacion vigente, que ya resuelve la jerarquia
     * empleado → sede → negocio. Sin compensacion configurada el porcentaje es
     * cero: el cargo se crea igual, con su historia, pero no le abona nada a
     * nadie. Inventar un porcentaje por defecto seria pagar de mas.</p>
     */
    private BigDecimal porcentajeDe(UUID employeeId, UUID branchId, UUID businessId) {
        EffectiveCompensation c = compensations
                .resolveForEmployee(employeeId, branchId, businessId)
                .orElse(null);
        if (c == null || c.getCompensationValue() == null) {
            log.warn("Empleado {} sin compensación vigente: sus cargos nacen en cero", employeeId);
            return BigDecimal.ZERO;
        }
        // SALARY_ONLY no reparte servicio: ese empleado cobra sueldo, no comision.
        if ("SALARY_ONLY".equals(c.getCompensationType())) return BigDecimal.ZERO;
        return c.getCompensationValue();
    }

    private static String legible(String estado) {
        return switch (estado) {
            case "CANCELADA_CLIENTE" -> "cancelada por el cliente";
            case "CANCELADA_NEGOCIO" -> "cancelada por el negocio";
            case "NO_ASISTIO" -> "inasistencia";
            case "EXPIRADA" -> "expirada sin confirmar";
            default -> estado;
        };
    }
}
