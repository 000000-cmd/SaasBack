package com.saas.system.domain.model.flow;

import com.saas.common.model.BaseDomain;
import lombok.*;

/**
 * Un texto de flujo, suelto a proposito.
 *
 * <p>NO se relaciona con una seccion ni con un control: se configura aqui y
 * quien lo necesite lo pide POR CODIGO. Atarlo a un control obligaria a
 * duplicar el mismo texto en cada sitio donde aparece, y a que cambiarlo en uno
 * lo dejara viejo en los otros.</p>
 *
 * <p>No son plantillas de notificacion: aquellas tienen destinatario, canal y
 * parametros; estas son lo que el bot o la pantalla dicen en un paso.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class FlowMessage extends BaseDomain {
    private String code;
    private String name;
    private String body;
    private String description;
}
