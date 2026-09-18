package com.saas.auth.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vinculo entre una CUENTA y un APARATO ("usuarios_vinculaciones").
 *
 * <p>No confundir con los dispositivos de push, que viven en events-service y
 * guardan tokens de FCM. Aqui lo que importa es {@code deviceId}: el serial
 * interno que genera el propio telefono y que la app guarda en el llavero del
 * sistema, no en sus datos. Sobrevive a desinstalar la app, borrar sus datos o
 * limpiar la cache — que es exactamente lo que un token de FCM no hace, y por
 * eso un token no sirve para responder "¿es el mismo aparato?".</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class UserDeviceLink extends BaseDomain {

    private UUID userId;
    private String deviceId;
    /** Como llamarlo al preguntarle a la persona: "Galaxy A54". */
    private String deviceName;
    /** ANDROID, IOS o WEB. */
    private String platform;
    private String appVersion;
    private LocalDateTime lastSeenAt;
    /** NULL = vinculo activo. */
    private LocalDateTime revokedAt;
    private UUID revokedBy;

    public boolean isActive() {
        return revokedAt == null && Boolean.TRUE.equals(getEnabled());
    }
}
