package com.saas.business.domain.model;

import com.saas.common.model.BaseDomain;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * La calificacion de UNA cita.
 *
 * <p>Una por cita —lo garantiza el indice unico {@code uq_ar_appointment}— y
 * solo sobre citas COMPLETADAS. Las dos reglas son la misma idea: una resena
 * tiene que respaldarse en un servicio que de verdad se presto. Sin eso, el
 * directorio se llena de estrellas de gente que nunca fue.</p>
 *
 * <p>Califica DOS cosas por separado: el negocio y quien atendio. Con una sola
 * nota no se distingue "el sitio esta sucio" de "no me gusto como me cortaron",
 * y son problemas de personas distintas.</p>
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class AppointmentReview extends BaseDomain {

    private UUID appointmentId;
    private UUID businessId;
    private UUID employeeId;

    /** 1 a 5. Es la que cuenta para el directorio. */
    private Integer businessStars;

    /** 1 a 5, opcional: se puede calificar el sitio sin calificar a la persona. */
    private Integer employeeStars;

    private String comment;

    /** {@code PUBLISHED} u {@code HIDDEN} (moderada por el administrador). */
    @Builder.Default private String status = "PUBLISHED";

    private String businessReply;
    private LocalDateTime businessRepliedAt;
}
