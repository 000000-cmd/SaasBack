package com.saas.system.domain.model.flow;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.util.UUID;

/**
 * Un paso del flujo.
 *
 * <p>El orden de las secciones ES el orden de los pasos: no hay otra tabla de
 * pasos, y el numero de paso es {@code displayOrder}.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class FlowSection extends BaseDomain {
    private UUID flowId;
    private String code;
    private String name;
    private String description;
    @Builder.Default private Integer displayOrder = 0;
    @Builder.Default private FlowSectionKind kind = FlowSectionKind.FORM;
    /** CSV de canales. Vacio = todos. */
    @Builder.Default private String channels = "WEB,PANEL,APK,WHATSAPP";
    @Builder.Default private Boolean isSystem = Boolean.FALSE;
}
