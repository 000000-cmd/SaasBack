package com.saas.system.infrastructure.persistence.repository;

import com.saas.system.infrastructure.persistence.entity.TourStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaTourStepRepository extends JpaRepository<TourStepEntity, UUID> {

    @Query("SELECT t FROM TourStepEntity t LEFT JOIN FETCH t.menu ORDER BY t.displayOrder")
    List<TourStepEntity> findAllOrdered();
}
