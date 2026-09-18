package com.saas.system.domain.model.flow;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.util.UUID;

/**
 * Un dato que pide una seccion.
 *
 * <p>{@code maskCode} apunta al ESTANDAR de mascaras, el mismo en web y en APK.
 * Configurar el tipo basta: de ahi salen tambien el teclado del movil, el tope
 * de caracteres y el ejemplo del placeholder.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class FlowField extends BaseDomain {
    private UUID sectionId;
    /** La clave con la que el dato viaja al servidor ("phone", "notes"). */
    private String code;
    private String label;
    private String placeholder;
    private String helpText;
    @Builder.Default private String dataType = "text";
    private String maskCode;
    /** De donde salen las opciones de un select. NULL cuando no aplica. */
    private String sourceKey;
    @Builder.Default private Boolean isRequired = Boolean.FALSE;
    @Builder.Default private String width = "full";
    @Builder.Default private Integer displayOrder = 0;
    @Builder.Default private Boolean isSystem = Boolean.FALSE;
}
