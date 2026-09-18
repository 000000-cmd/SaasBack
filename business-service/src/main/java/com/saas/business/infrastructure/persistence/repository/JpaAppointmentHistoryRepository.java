package com.saas.business.infrastructure.persistence.repository;

import com.saas.business.infrastructure.persistence.entity.AppointmentHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaAppointmentHistoryRepository extends JpaRepository<AppointmentHistoryEntity, UUID> {

    List<AppointmentHistoryEntity> findByAppointmentIdOrderByOccurredAtAsc(UUID appointmentId);
}
