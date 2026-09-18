package com.saas.events.domain.model;

import com.saas.common.model.BaseDomain;
import com.saas.common.model.ICodeable;
import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class NotificationTemplate extends BaseDomain implements ICodeable {
    private String code;
    private String name;
    private String typeCode;
    /** Solo aplica a EMAIL; en SMS y WhatsApp se ignora. */
    private String subject;
    private String body;
    /** NULL mientras la plantilla no este asociada a una notificacion. */
    private UUID notificationId;
}
