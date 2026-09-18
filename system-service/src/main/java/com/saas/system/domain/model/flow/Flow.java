package com.saas.system.domain.model.flow;

import com.saas.common.model.BaseDomain;
import lombok.*;

/**
 * Un flujo configurable. El agendamiento (AGEND) es el primero.
 *
 * <p>Lo consume el front POR CODIGO. De aqui salen que pasos hay y que datos se
 * piden, para que la web, el panel, el APK y WhatsApp pregunten lo mismo en el
 * mismo orden sin que nadie tenga que acordarse de tocar cuatro sitios.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class Flow extends BaseDomain {
    private String code;
    private String name;
    private String description;
    /** Los del sistema no se borran: hay codigo que los invoca por su codigo. */
    @Builder.Default private Boolean isSystem = Boolean.FALSE;
}
