package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.BusinessPublicProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaBusinessPublicProfileRepository
        extends JpaRepository<BusinessPublicProfileEntity, UUID> {

    Optional<BusinessPublicProfileEntity> findByBusinessId(UUID businessId);

    /**
     * Suma una resena al agregado, EN LA BASE.
     *
     * <p>Leer la media, calcularla y escribirla se pierde cuando dos resenas
     * llegan a la vez: las dos leen lo mismo y la segunda pisa a la primera.
     * Con {@code StarSum = StarSum + :estrellas} el incremento lo resuelve el
     * motor, y no hay ventana entre leer y escribir.</p>
     *
     * <p>El negocio entra como {@code String} y no como {@code UUID}: la columna
     * es {@code CHAR(36)} y en una consulta NATIVA un UUID se enlaza en binario,
     * asi que no casa con ninguna fila. No falla — actualiza cero filas, que es
     * la peor forma de fallar. El resto de consultas nativas del proyecto pasan
     * String por lo mismo.</p>
     *
     * @return filas afectadas; 0 si ese negocio aun no tiene ficha publica.
     */
    @Modifying
    @Query(value = "UPDATE business_public_profile "
                 + "SET ReviewCount = ReviewCount + 1, StarSum = StarSum + :estrellas, "
                 + "    AuditDate = NOW(6) "
                 + "WHERE BusinessId = :businessId AND Visible = 1",
           nativeQuery = true)
    int addReview(@Param("businessId") String businessId, @Param("estrellas") int estrellas);
}
