package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * La marca de que un aviso de una cita YA salio.
 *
 * <p>Existe para no avisar dos veces. Es la deduplicacion por (cita, tipo de
 * aviso) que pide la especificacion, y la garantiza el indice unico de la
 * tabla: si el barrido de recordatorios corre dos veces —o dos instancias
 * corren a la vez—, el segundo INSERT no inserta nada.</p>
 *
 * <p>{@code notificationCode} lleva sufijo en los recordatorios
 * ({@code APPOINTMENT_REMINDER:24}): el de 24 horas y el de 2 son avisos
 * distintos, y con una sola marca por cita el segundo nunca saldria.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class AppointmentNotice extends BaseDomain {
    private UUID appointmentId;
    private String notificationCode;
    private LocalDateTime sentAt;
}
