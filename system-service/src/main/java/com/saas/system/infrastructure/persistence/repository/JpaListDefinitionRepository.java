package com.saas.system.infrastructure.persistence.repository;

import com.saas.system.infrastructure.persistence.entity.ListDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para ListDefinitionEntity.
 */
@Repository
public interface JpaListDefinitionRepository extends JpaRepository<ListDefinitionEntity, UUID> {

    /**
     * Busca una definición por el nombre de la tabla física.
     */
    Optional<ListDefinitionEntity> findByPhysicalTableName(String physicalTableName);

    /**
     * Verifica si existe una definición con el nombre de tabla dado.
     */
    boolean existsByPhysicalTableName(String physicalTableName);

    /**
     * Obtiene todas las definiciones visibles.
     */
    List<ListDefinitionEntity> findByVisibleTrue();
}