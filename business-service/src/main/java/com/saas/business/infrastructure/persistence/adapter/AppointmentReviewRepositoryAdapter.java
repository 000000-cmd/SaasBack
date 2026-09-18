package com.saas.business.infrastructure.persistence.adapter;

import com.saas.business.domain.model.AppointmentReview;
import com.saas.business.domain.port.out.IAppointmentReviewRepositoryPort;
import com.saas.business.infrastructure.persistence.entity.AppointmentReviewEntity;
import com.saas.business.infrastructure.persistence.mapper.AppointmentReviewPersistenceMapper;
import com.saas.business.infrastructure.persistence.repository.JpaAppointmentReviewRepository;
import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AppointmentReviewRepositoryAdapter
        extends BaseJpaRepositoryAdapter<AppointmentReview, AppointmentReviewEntity, UUID>
        implements IAppointmentReviewRepositoryPort {

    private final JpaAppointmentReviewRepository jpa;

    public AppointmentReviewRepositoryAdapter(JpaAppointmentReviewRepository jpa,
                                              AppointmentReviewPersistenceMapper mapper) {
        super(jpa, mapper, "Reseña");
        this.jpa = jpa;
    }

    @Override
    public Optional<AppointmentReview> findByAppointment(UUID appointmentId) {
        return jpa.findByAppointmentId(appointmentId).map(getMapper()::toDomain);
    }

    @Override
    public List<AppointmentReview> publishedOfBusiness(UUID businessId, int limite) {
        return getMapper().toDomainList(jpa.published(businessId, PageRequest.of(0, limite)));
    }

    @Override
    public List<AppointmentReview> publishedOfEmployee(UUID employeeId, int limite) {
        return getMapper().toDomainList(jpa.ofEmployee(employeeId, PageRequest.of(0, limite)));
    }

    @Override
    @Transactional
    public boolean saveIfFirst(AppointmentReview r) {
        return jpa.insertIfAbsent(
                UUID.randomUUID().toString(),
                r.getAppointmentId().toString(),
                r.getBusinessId().toString(),
                r.getEmployeeId().toString(),
                r.getBusinessStars(),
                r.getEmployeeStars(),
                r.getComment()) == 1;
    }
}
