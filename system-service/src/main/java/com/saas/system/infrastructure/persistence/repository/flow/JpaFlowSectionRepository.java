package com.saas.system.infrastructure.persistence.repository.flow;

import com.saas.system.infrastructure.persistence.entity.flow.FlowSectionEntity;
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
public interface JpaFlowSectionRepository extends JpaRepository<FlowSectionEntity, UUID> {
    List<FlowSectionEntity> findByFlowIdOrderByDisplayOrderAsc(UUID flowId);
    Optional<FlowSectionEntity> findByFlowIdAndCode(UUID flowId, String code);

    /**
     * Reactiva la fila borrada que tenga ese codigo dentro del flujo, si la hay.
     *
     * <p>Nativa a proposito: la clave unica es (FlowId, Code) SIN mirar Visible,
     * asi que borrar y volver a crear con el mismo codigo choca contra el
     * indice. Y con JPA la fila no se ve, porque {@code @SQLRestriction} la
     * filtra en toda consulta.</p>
     *
     * <p>Devuelve 0 cuando no habia nada borrado; entonces —y solo entonces—
     * hay que insertar.</p>
     */
    @Modifying
    @Query(value = "UPDATE flow_section SET Visible = 1, Enabled = 1, Name = :name, "
                 + "AuditDate = NOW(6) WHERE FlowId = :flowId AND Code = :code",
           nativeQuery = true)
    int revive(@Param("flowId") String flowId,
               @Param("code") String code,
               @Param("name") String name);
}
