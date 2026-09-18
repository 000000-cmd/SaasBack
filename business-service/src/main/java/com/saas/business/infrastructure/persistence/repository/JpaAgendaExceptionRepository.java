package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.AgendaExceptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaAgendaExceptionRepository extends JpaRepository<AgendaExceptionEntity, UUID> {

    /**
     * Lo que resta agenda en el negocio dentro del rango.
     *
     * <p>Solapamiento con extremo abierto: {@code inicio < fin_rango} y
     * {@code inicio_rango < fin}. Con {@code <=} en cualquiera de los dos, una
     * excepcion que termina justo cuando empieza el rango contaria como que lo
     * pisa, y no lo pisa.</p>
     */
    @Query("SELECT a FROM AgendaExceptionEntity a "
         + "WHERE a.businessId = :businessId AND a.enabled = true "
         + "AND a.startUtc < :end AND :start < a.endUtc "
         + "ORDER BY a.startUtc ASC")
    List<AgendaExceptionEntity> overlapping(@Param("businessId") UUID businessId,
                                            @Param("start") Instant start,
                                            @Param("end") Instant end);
}
