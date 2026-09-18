package com.saas.system.infrastructure.persistence.repository.flow;

import com.saas.system.infrastructure.persistence.entity.flow.FlowFieldEntity;
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
public interface JpaFlowFieldRepository extends JpaRepository<FlowFieldEntity, UUID> {
    /** Por lote: un flujo tiene varias secciones y pedirlas una a una son N viajes. */
    List<FlowFieldEntity> findBySectionIdInOrderByDisplayOrderAsc(Collection<UUID> sectionIds);
    List<FlowFieldEntity> findBySectionIdOrderByDisplayOrderAsc(UUID sectionId);

    /**
     * Reactiva la fila borrada que tenga ese codigo dentro de la seccion, si la hay.
     *
     * <p>Nativa a proposito: la clave unica es (SectionId, Code) SIN mirar Visible,
     * asi que borrar y volver a crear con el mismo codigo choca contra el
     * indice. Y con JPA la fila no se ve, porque {@code @SQLRestriction} la
     * filtra en toda consulta.</p>
     *
     * <p>Devuelve 0 cuando no habia nada borrado; entonces —y solo entonces—
     * hay que insertar.</p>
     */
    @Modifying
    @Query(value = "UPDATE flow_field SET Visible = 1, Enabled = 1, Label = :label, "
                 + "AuditDate = NOW(6) WHERE SectionId = :sectionId AND Code = :code",
           nativeQuery = true)
    int revive(@Param("sectionId") String sectionId,
               @Param("code") String code,
               @Param("label") String label);
}
