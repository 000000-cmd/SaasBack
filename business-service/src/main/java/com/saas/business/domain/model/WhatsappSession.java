package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Una conversacion de WhatsApp a medias.
 *
 * <p>Guarda EN QUE PASO va y lo que lleva recogido. Los pasos y los campos los
 * decide la configuracion del flujo, asi que lo recogido viaja en JSON: una
 * columna por dato obligaria a migrar la tabla cada vez que alguien anada una
 * pregunta al flujo.</p>
 *
 * <p>Caduca, y eso NO es limpieza: una conversacion a medias de hace tres dias
 * no es la misma conversacion. Sin caducidad, quien escribe "hola" despues de
 * una semana se encuentra contestando la pregunta cuatro.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class WhatsappSession extends BaseDomain {
    private UUID businessId;
    private String phoneE164;
    @Builder.Default private String flowCode = "AGEND";
    /** Codigo de la seccion en la que esta. Nulo = sin empezar. */
    private String stepCode;
    private String dataJson;
    private LocalDateTime expiresAt;
}
