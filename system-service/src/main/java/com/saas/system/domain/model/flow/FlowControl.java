package com.saas.system.domain.model.flow;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.util.UUID;

/**
 * Un boton de una seccion.
 *
 * <p>{@code permissionCode} se resuelve contra {@code permission} y
 * {@code role_permission}, que ya existen: este modulo NO trae su propio
 * mecanismo de permisos.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class FlowControl extends BaseDomain {
    private UUID sectionId;
    private String code;
    private String label;
    @Builder.Default private FlowControlAction action = FlowControlAction.CUSTOM;
    @Builder.Default private String variant = "primary";
    /** NULL = cualquiera que llegue a la pantalla. */
    private String permissionCode;
    @Builder.Default private String channels = "WEB,PANEL,APK,WHATSAPP";
    @Builder.Default private Integer displayOrder = 0;
    @Builder.Default private Boolean isSystem = Boolean.FALSE;
}
