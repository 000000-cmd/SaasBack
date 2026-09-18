package com.saas.business.domain.port.out;

import com.saas.business.domain.model.AppointmentReview;
import com.saas.common.port.out.IGenericRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IAppointmentReviewRepositoryPort
        extends IGenericRepositoryPort<AppointmentReview, UUID> {

    Optional<AppointmentReview> findByAppointment(UUID appointmentId);

    List<AppointmentReview> publishedOfBusiness(UUID businessId, int limite);

    List<AppointmentReview> publishedOfEmployee(UUID employeeId, int limite);

    /**
     * Deja la resena si esa cita no tenia ninguna. {@code false} = ya opino.
     *
     * <p>No lanza al chocar: la insercion es {@code INSERT IGNORE}. Una
     * excepcion de clave duplicada marcaria para deshacer la transaccion en la
     * que ademas se actualiza el agregado.</p>
     */
    boolean saveIfFirst(AppointmentReview review);
}
