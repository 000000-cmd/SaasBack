package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.AppointmentNoticeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface JpaAppointmentNoticeRepository extends JpaRepository<AppointmentNoticeEntity, UUID> {

    /**
     * Deja la marca si no estaba. {@code INSERT IGNORE} y no un try/catch.
     *
     * <p>Capturar la excepcion de clave duplicada no sirve: para cuando llega,
     * la transaccion ya esta marcada para deshacerse, y quien llama esta dentro
     * de la transaccion que crea la cita. Un aviso repetido no puede costar la
     * reserva.</p>
     *
     * @return 1 la primera vez, 0 si ya estaba.
     */
    @Modifying
    @Query(value = "INSERT IGNORE INTO appointment_notification "
                 + "(Id, AppointmentId, NotificationCode, SentAt, Enabled, Visible, AuditDate, CreatedDate) "
                 + "VALUES (:id, :appointmentId, :code, NOW(6), 1, 1, NOW(6), NOW(6))",
           nativeQuery = true)
    int insertIfAbsent(@Param("id") String id,
                       @Param("appointmentId") String appointmentId,
                       @Param("code") String code);
}
