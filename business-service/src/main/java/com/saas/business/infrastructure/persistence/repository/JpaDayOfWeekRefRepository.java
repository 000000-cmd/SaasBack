package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.DayOfWeekRefEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaDayOfWeekRefRepository extends JpaRepository<DayOfWeekRefEntity, UUID> {
}
