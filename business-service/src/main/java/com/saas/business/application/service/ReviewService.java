package com.saas.business.application.service;

import com.saas.business.domain.model.Appointment;
import com.saas.business.domain.model.AppointmentReview;
import com.saas.business.domain.model.AppointmentStatus;
import com.saas.business.domain.model.BusinessClient;
import com.saas.business.domain.model.PublicCode;
import com.saas.business.domain.port.out.IAppointmentRepositoryPort;
import com.saas.business.domain.port.out.IAppointmentReviewRepositoryPort;
import com.saas.business.domain.port.out.IBusinessClientRepositoryPort;
import com.saas.business.domain.port.out.IBusinessPublicProfileRepositoryPort;
import com.saas.common.exception.BusinessException;
import com.saas.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Las calificaciones.
 *
 * <h3>Solo se califica lo que se prestó</h3>
 * <p>La reseña cuelga de una cita {@code COMPLETADA}, y de ninguna otra cosa.
 * Es lo que separa un directorio de un muro de opiniones: sin ese anclaje,
 * cualquiera puede hundir a un negocio al que nunca fue, y el propio negocio
 * puede inflarse solo.</p>
 *
 * <h3>Quién puede opinar</h3>
 * <p>El cliente no tiene cuenta, así que se identifica igual que para consultar
 * su cita: <b>código público + los últimos cuatro dígitos de su celular</b>. El
 * código dice cuál es la cita; el teléfono dice que es suya. Con el código solo
 * bastaría con acertar uno para opinar en nombre de un desconocido.</p>
 *
 * <h3>Una por cita</h3>
 * <p>Lo garantiza el índice único, no un {@code if}: dos envíos simultáneos del
 * mismo formulario pasarían los dos la comprobación previa. El segundo choca
 * contra la base y se responde que ya opinó — que es la verdad.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final IAppointmentRepositoryPort appointments;
    private final IAppointmentReviewRepositoryPort reviews;
    private final IBusinessPublicProfileRepositoryPort profiles;
    private final IBusinessClientRepositoryPort clients;

    /** Lo que llega del formulario público. */
    public record NuevaResena(String publicCode, String last4,
                              Integer businessStars, Integer employeeStars, String comment) {}

    /**
     * Deja la reseña de una cita.
     *
     * @param businessId el negocio del subdominio por el que se entró. La cita
     *        tiene que ser suya: sin esta comprobación se podría reseñar la cita
     *        de otro negocio desde la página de éste.
     */
    @Transactional
    public AppointmentReview crear(UUID businessId, NuevaResena req) {
        int estrellas = validarEstrellas(req.businessStars(), "del negocio");
        if (req.employeeStars() != null) validarEstrellas(req.employeeStars(), "del profesional");

        Appointment cita = appointments.findByPublicCode(PublicCode.normalize(req.publicCode()))
                .filter(a -> businessId.equals(a.getBusinessId()))
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "código", req.publicCode()));

        exigirDuenoDelTelefono(cita, req.last4());

        if (cita.getStatus() != AppointmentStatus.COMPLETADA) {
            throw new BusinessException(
                    "Solo se puede calificar un servicio que ya se prestó.");
        }

        AppointmentReview nueva = AppointmentReview.builder()
                .appointmentId(cita.getId())
                .businessId(cita.getBusinessId())
                .employeeId(cita.getEmployeeId())
                .businessStars(estrellas)
                .employeeStars(req.employeeStars())
                .comment(recortar(req.comment()))
                .build();

        if (!reviews.saveIfFirst(nueva)) {
            throw new BusinessException("Ya dejaste tu opinión de esta cita. Gracias.");
        }

        // El agregado se suma DESPUÉS de que la reseña entrara, y solo si
        // entró: al revés, un choque de clave única dejaría la media contando
        // una opinión que no existe.
        if (!profiles.addReview(cita.getBusinessId(), estrellas)) {
            // Sin ficha pública la reseña se guarda igual — el negocio puede
            // publicarse mañana y sus opiniones ya estarán ahí. Lo que no se
            // puede es perderlas por no estar listado hoy.
            log.info("Negocio {} sin ficha pública: la reseña queda guardada sin agregado",
                    cita.getBusinessId());
        }

        return reviews.findByAppointment(cita.getId()).orElse(nueva);
    }

    /** Si esa cita ya tiene reseña. Lo consulta la pantalla antes de ofrecerla. */
    @Transactional(readOnly = true)
    public boolean yaCalificada(UUID appointmentId) {
        return reviews.findByAppointment(appointmentId).isPresent();
    }

    /**
     * Los últimos cuatro del celular, igual que para consultar la cita.
     *
     * <p>Un cliente de mostrador no dejó teléfono: no puede demostrar que la
     * cita es suya, así que no puede opinar. Es incómodo y es correcto — la
     * alternativa es que cualquiera con el código opine por él.</p>
     */
    private void exigirDuenoDelTelefono(Appointment cita, String last4) {
        BusinessClient cliente = clients.findById(cita.getBusinessClientId()).orElse(null);
        String tel = cliente == null ? null : cliente.getPhoneE164();
        if (tel == null || tel.length() < 4) {
            throw new BusinessException(
                    "Esta cita no tiene un celular con el que comprobar que es tuya.");
        }
        String esperado = tel.substring(tel.length() - 4);
        if (last4 == null || !esperado.equals(last4.trim())) {
            // El mismo 404 que un código inexistente: decir "el código está
            // bien pero el teléfono no" ya confirma que esa cita existe.
            throw new ResourceNotFoundException("Cita", "código", cita.getPublicCode());
        }
    }

    private static int validarEstrellas(Integer v, String cual) {
        if (v == null || v < 1 || v > 5) {
            throw new BusinessException("La calificación " + cual + " va de 1 a 5 estrellas");
        }
        return v;
    }

    private static String recortar(String comentario) {
        if (comentario == null) return null;
        String limpio = comentario.trim();
        if (limpio.isEmpty()) return null;
        return limpio.length() > 1000 ? limpio.substring(0, 1000) : limpio;
    }
}
