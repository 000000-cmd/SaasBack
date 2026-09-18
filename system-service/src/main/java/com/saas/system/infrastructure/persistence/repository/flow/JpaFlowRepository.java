package com.saas.system.infrastructure.persistence.repository.flow;

import com.saas.system.infrastructure.persistence.entity.flow.FlowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaFlowRepository extends JpaRepository<FlowEntity, UUID> {
    Optional<FlowEntity> findByCode(String code);
    boolean existsByCode(String code);
    List<FlowEntity> findAllByOrderByCodeAsc();

    /**
     * Reactiva la fila borrada que tenga ese codigo, si la hay.
     *
     * <p>Nativa a proposito: la clave unica es (Code) SIN mirar Visible,
     * asi que borrar y volver a crear con el mismo codigo choca contra el
     * indice. Y con JPA la fila no se ve, porque {@code @SQLRestriction} la
     * filtra en toda consulta.</p>
     *
     * <p>Devuelve 0 cuando no habia nada borrado; entonces —y solo entonces—
     * hay que insertar.</p>
     */
    @Modifying
    @Query(value = "UPDATE flow SET Visible = 1, Enabled = 1, Name = :name, "
                 + "Description = :description, AuditDate = NOW(6) WHERE Code = :code",
           nativeQuery = true)
    int reviveByCode(@Param("code") String code,
                     @Param("name") String name,
                     @Param("description") String description);
}
