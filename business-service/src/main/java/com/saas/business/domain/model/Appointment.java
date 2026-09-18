package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.ITenantOwned;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Una cita.
 *
 * <h3>El tiempo</h3>
 * <p>{@code startUtc} y {@code endUtc} son SIEMPRE UTC; toda comparacion del
 * motor ocurre ahi. {@code businessTimeZone} se congela al agendar para poder
 * reconstruir la hora local historica: si el negocio se muda de huso, las citas
 * viejas siguen contando la hora a la que de verdad ocurrieron.</p>
 *
 * <p>{@code localDate} se deriva de las dos anteriores y existe por dos motivos
 * concretos: es la clave del cerrojo anti doble reserva, y es como se consulta
 * la agenda ("el dia 5"), que en UTC seria un rango que cruza dos fechas.</p>
 *
 * <h3>Agendar y registrar no son lo mismo</h3>
 * <p>{@code backdated = false} es AGENDAR algo futuro: exclusivo, una franja y
 * una persona. {@code backdated = true} es REGISTRAR algo que ya ocurrio, y eso
 * si puede solaparse con lo que hubiera — el pasado no se negocia, se anota.
 * Son dos operaciones con validaciones distintas, no una bandera para saltarse
 * la comprobacion.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Appointment extends BaseDomain implements ITenantOwned {

    private UUID businessId;
    private UUID branchId;
    private UUID employeeId;
    private UUID businessClientId;

    private AppointmentChannel channel;
    /** Nulo cuando la creo el cliente sin cuenta desde la web publica. */
    private UUID createdByUserId;

    private Instant startUtc;
    private Instant endUtc;
    private String businessTimeZone;
    private LocalDate localDate;

    private AppointmentStatus status;
    /** Bloqueo optimista: dos pantallas editando la misma cita no se pisan. */
    private Integer version;

    /** Codigo corto y no secuencial. Es lo unico que se le da al cliente. */
    private String publicCode;

    private boolean backdated;

    /**
     * De donde viene, si es la sustituta de una reprogramada.
     *
     * <p>Reprogramar no mueve fechas sobre la misma fila: cancela la original
     * con su motivo y crea esta apuntando alli. Asi la historia queda entera.</p>
     */
    private UUID rescheduledFromAppointmentId;

    private String cancelReason;
    private CancelledBy cancelledBy;
    private Instant cancelledAt;
    private Instant startedAt;
    private Instant completedAt;

    private BigDecimal totalPrice;
    private Integer totalDurationMinutes;
    private String currency;
    private String notes;

    /**
     * Las lineas de servicio con su foto del momento. No se persisten desde
     * aqui: las guarda su propio repositorio.
     */
    @Builder.Default
    private List<AppointmentLine> lines = new ArrayList<>();

    /** Ocupa agenda y por tanto cuenta para el solapamiento. */
    public boolean occupiesAgenda() {
        return status != null && status.occupiesAgenda();
    }

    /** El rango que ocupa, para comprobar solapes. */
    public boolean overlaps(Instant otherStart, Instant otherEnd) {
        return startUtc.isBefore(otherEnd) && otherStart.isBefore(endUtc);
    }
}
