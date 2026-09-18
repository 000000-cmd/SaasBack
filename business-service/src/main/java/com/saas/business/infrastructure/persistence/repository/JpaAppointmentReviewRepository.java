package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.AppointmentReviewEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaAppointmentReviewRepository extends JpaRepository<AppointmentReviewEntity, UUID> {

    Optional<AppointmentReviewEntity> findByAppointmentId(UUID appointmentId);

    @Query("SELECT r FROM AppointmentReviewEntity r "
         + "WHERE r.businessId = :businessId AND r.status = 'PUBLISHED' "
         + "ORDER BY r.createdDate DESC")
    List<AppointmentReviewEntity> published(@Param("businessId") UUID businessId, Pageable page);

    @Query("SELECT r FROM AppointmentReviewEntity r "
         + "WHERE r.employeeId = :employeeId AND r.status = 'PUBLISHED' "
         + "ORDER BY r.createdDate DESC")
    List<AppointmentReviewEntity> ofEmployee(@Param("employeeId") UUID employeeId, Pageable page);

    /**
     * Deja la resena si esa cita no tenia ninguna.
     *
     * <p>{@code INSERT IGNORE} y no un try/catch: el choque contra
     * {@code uq_ar_appointment} dentro de una transaccion la marca para
     * deshacerse, y quien llama esta guardando tambien el agregado. Aqui el
     * choque significa "ya opino", que no es un error.</p>
     *
     * @return 1 la primera vez, 0 si ya habia resena.
     */
    @Modifying
    @Query(value = "INSERT IGNORE INTO appointment_review "
                 + "(Id, AppointmentId, BusinessId, EmployeeId, BusinessStars, EmployeeStars, "
                 + " Comment, Status, Enabled, Visible, AuditDate, CreatedDate) "
                 + "VALUES (:id, :appointmentId, :businessId, :employeeId, :businessStars, "
                 + " :employeeStars, :comment, 'PUBLISHED', 1, 1, NOW(6), NOW(6))",
           nativeQuery = true)
    int insertIfAbsent(@Param("id") String id,
                       @Param("appointmentId") String appointmentId,
                       @Param("businessId") String businessId,
                       @Param("employeeId") String employeeId,
                       @Param("businessStars") int businessStars,
                       @Param("employeeStars") Integer employeeStars,
                       @Param("comment") String comment);
}
