package com.saas.business.application.service;

import com.saas.business.domain.availability.BookingPolicy;
import com.saas.business.domain.model.AppointmentChannel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Todo lo necesario para crear una cita, resuelto YA.
 *
 * <p>El servicio de reservas no busca precios ni politicas por su cuenta: los
 * recibe. Asi la parte dificil —el cerrojo, el solapamiento, la transaccion—
 * se puede probar sin montar medio catalogo, y los importes que se congelan son
 * exactamente los que quien llama decidio congelar.</p>
 *
 * @param backdated {@code true} = REGISTRAR algo que ya ocurrio (puede
 *        solaparse). {@code false} = AGENDAR algo futuro (exclusivo). Son dos
 *        operaciones con validaciones distintas, no una bandera para saltarse
 *        la comprobacion.
 * @param now el instante actual, inyectado. Sin esto, "no reservar con menos de
 *        30 minutos" no se puede probar.
 */
public record BookingCommand(
        UUID businessId,
        UUID branchId,
        UUID employeeId,
        UUID businessClientId,
        AppointmentChannel channel,
        UUID actorUserId,
        Instant startUtc,
        ZoneId zone,
        List<Line> lines,
        boolean backdated,
        boolean requiresManualConfirmation,
        BookingPolicy policy,
        Instant now,
        String notes,
        UUID rescheduledFromAppointmentId
) {
    /**
     * Un servicio de la cita, con los importes ya resueltos de la compensacion
     * vigente. Se congelan tal cual: si el dueño sube el precio en julio, las
     * citas de marzo no pueden cambiar de valor.
     */
    public record Line(
            UUID offeringId,
            String serviceName,
            BigDecimal price,
            int durationMinutes,
            BigDecimal commissionRate
    ) {
        /** Lo que se lleva el empleado, calculado una vez y congelado. */
        public BigDecimal commissionAmount() {
            if (price == null || commissionRate == null) return BigDecimal.ZERO;
            return price.multiply(commissionRate)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        }
    }

    public int totalMinutes() {
        return lines.stream().mapToInt(Line::durationMinutes).sum();
    }

    public BigDecimal totalPrice() {
        return lines.stream()
                .map(Line::price)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
