package com.saas.events.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un telefono (o navegador) suscrito a push.
 *
 * El token es unico a proposito: el mismo aparato puede cambiar de dueno cuando
 * alguien cierra sesion y entra otro. Al registrarlo se REASIGNA, nunca se
 * duplica — si no, el dueno anterior seguiria recibiendo notificaciones ajenas.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class UserDevice extends BaseDomain {

    private UUID thirdPartyId;
    private String fcmToken;
    /** ANDROID, IOS o WEB. */
    private String platform;
    private String appVersion;
    private LocalDateTime lastSeenAt;
}
