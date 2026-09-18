package com.saas.events.domain.port.out;

import com.saas.common.dto.PagedResponse;
import com.saas.common.port.out.IGenericRepositoryPort;
import com.saas.events.domain.model.NotificationLog;
import com.saas.events.domain.model.SendStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface INotificationLogRepositoryPort extends IGenericRepositoryPort<NotificationLog, UUID> {

    /**
     * Guarda la bitacora y la BAJA A LA BASE en el acto.
     *
     * <p>Hace falta el flush explicito porque el id se asigna en Java: sin el,
     * Hibernate aplaza el INSERT hasta el commit y el choque contra
     * uq_log_event_template salta FUERA del try que lo interpreta como "este
     * evento ya se proceso". Al escaparse, el consumidor no acusa recibo y
     * Kafka reentrega el mismo mensaje para siempre.</p>
     */
    NotificationLog saveNow(NotificationLog row);

    /** Conteo de filas por estado, para las estadisticas. */
    Map<SendStatus, Long> countByStatus();

    /** Cuenta filas creadas desde una fecha (sirve para "hoy" y "mes"). */
    long countSince(LocalDateTime from);

    /** Serie diaria de enviados/fallidos desde una fecha, para el grafico de estadisticas. */
    List<DailyCount> dailySince(LocalDateTime from);

    /** Los 5 fallos mas recientes, para el panel de estadisticas. */
    List<NotificationLog> lastFailures();

    /** Busqueda paginada con filtros opcionales, para GET /notification/logs. */
    PagedResponse<NotificationLog> search(SendStatus status, String recipient, int page, int size);

    /** Purga por antiguedad. Devuelve cuantas filas se borraron. Lo llama LogPurgeJob. */
    long deleteOlderThan(LocalDateTime cutoff);

    record DailyCount(LocalDate date, long sent, long failed) {}
}
