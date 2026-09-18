package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Un cambio de estado de una cita. SOLO SE AÑADE.
 *
 * <p>Sin esto no hay forma de responder "quien canceló esto y cuando", que es
 * exactamente la pregunta que aparece cuando un cliente reclama. Por eso no se
 * modifica ni se borra: se anota lo que pasó, con quien y por donde.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AppointmentHistoryEntry extends BaseDomain {

    private UUID appointmentId;
    /** Nulo en la primera fila: la cita no venia de ningun estado. */
    private AppointmentStatus fromStatus;
    private AppointmentStatus toStatus;
    private AppointmentChannel channel;
    /** Nulo si lo hizo el cliente sin cuenta o un proceso automatico. */
    private UUID actorUserId;
    private String reason;
    private Instant occurredAt;
}
