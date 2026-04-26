package com.saas.system.infrastructure.persistence.repository;

import com.saas.system.infrastructure.persistence.entity.SystemListItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaSystemListItemRepository extends JpaRepository<SystemListItemEntity, UUID> {

    @Query("SELECT i FROM SystemListItemEntity i WHERE i.list.id = :listId ORDER BY i.displayOrder")
    List<SystemListItemEntity> findByListId(@Param("listId") UUID listId);

    @Query("SELECT i FROM SystemListItemEntity i WHERE i.list.id = :listId AND i.code = :code")
    Optional<SystemListItemEntity> findByListIdAndCode(@Param("listId") UUID listId, @Param("code") String code);

    @Query("SELECT (COUNT(i) > 0) FROM SystemListItemEntity i WHERE i.list.id = :listId AND i.code = :code")
    boolean existsByListIdAndCode(@Param("listId") UUID listId, @Param("code") String code);
}
