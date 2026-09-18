package com.saas.events.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Una notificacion tal y como la ve su destinatario.
 *
 * NO es un canal: es la superficie de LECTURA. Se escribe una fila por
 * destinatario en TODO envio, salga por correo, SMS, WhatsApp o push, de modo
 * que la campana de la web y la lista del movil lean exactamente lo mismo.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class NotificationInbox extends BaseDomain {

    /** Tercero destinatario (cliente, empleado o dueno). Vive en saas_db. */
    private UUID thirdPartyId;
    private String notificationCode;
    private String templateCode;
    private String typeCode;
    private String title;
    private String body;

    /** NULL mientras no se haya leido. */
    private LocalDateTime readAt;

    public boolean isUnread() { return readAt == null; }
}
