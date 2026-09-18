package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.WhatsappMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaWhatsappMessageRepository extends JpaRepository<WhatsappMessageEntity, UUID> {

    Optional<WhatsappMessageEntity> findByWaMessageId(String waMessageId);

    /**
     * Guarda el mensaje si su id de Meta no estaba.
     *
     * <p>Meta REENTREGA los webhooks; sin esto una reentrega crearia una
     * segunda cita. {@code INSERT IGNORE} y no un try/catch: el choque dentro
     * de una transaccion la marca para deshacerse, y aqui el choque significa
     * "ya lo tenemos", que no es un error.</p>
     *
     * @return 1 la primera vez, 0 si es una reentrega.
     */
    @Modifying
    @Query(value = "INSERT IGNORE INTO whatsapp_message "
                 + "(Id, WaMessageId, BusinessId, FromPhone, ToPhone, Body, RawPayload, "
                 + " ReceivedAt, Enabled, Visible, AuditDate, CreatedDate) "
                 + "VALUES (:id, :waId, :businessId, :from, :to, :body, :raw, "
                 + " NOW(6), 1, 1, NOW(6), NOW(6))",
           nativeQuery = true)
    int insertIfAbsent(@Param("id") String id,
                       @Param("waId") String waMessageId,
                       @Param("businessId") String businessId,
                       @Param("from") String fromPhone,
                       @Param("to") String toPhone,
                       @Param("body") String body,
                       @Param("raw") String rawPayload);

    @Modifying
    @Query(value = "UPDATE whatsapp_message SET ProcessedAt = NOW(6), Error = :error, "
                 + "AuditDate = NOW(6) WHERE WaMessageId = :waId", nativeQuery = true)
    int markProcessed(@Param("waId") String waMessageId, @Param("error") String error);
}
