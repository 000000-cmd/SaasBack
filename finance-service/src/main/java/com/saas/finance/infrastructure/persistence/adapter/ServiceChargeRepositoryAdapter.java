package com.saas.finance.infrastructure.persistence.adapter;

import com.saas.common.persistence.BaseJpaRepositoryAdapter;
import com.saas.finance.domain.model.ChargeStatus;
import com.saas.finance.domain.model.ServiceCharge;
import com.saas.finance.domain.port.out.IServiceChargeRepositoryPort;
import com.saas.finance.infrastructure.persistence.entity.ServiceChargeEntity;
import com.saas.finance.infrastructure.persistence.mapper.ServiceChargePersistenceMapper;
import com.saas.finance.infrastructure.persistence.repository.JpaServiceChargeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public class ServiceChargeRepositoryAdapter
        extends BaseJpaRepositoryAdapter<ServiceCharge, ServiceChargeEntity, UUID>
        implements IServiceChargeRepositoryPort {

    private final JpaServiceChargeRepository jpa;

    public ServiceChargeRepositoryAdapter(JpaServiceChargeRepository jpa,
                                          ServiceChargePersistenceMapper mapper) {
        super(jpa, mapper, "Servicio prestado");
        this.jpa = jpa;
    }

    @Override public List<ServiceCharge> findByAppointments(Collection<UUID> appointmentIds) {
        if (appointmentIds == null || appointmentIds.isEmpty()) return List.of();
        return getMapper().toDomainList(jpa.findByAppointmentIdIn(appointmentIds));
    }

    @Override public List<ServiceCharge> findByEmployee(UUID employeeId) {
        return getMapper().toDomainList(jpa.findByEmployeeIdOrderByServiceDateDescStartTimeDesc(employeeId));
    }

    @Override public List<ServiceCharge> findSettlable(UUID employeeId) {
        return getMapper().toDomainList(
                jpa.findByEmployeeIdAndStatusAndSettlementIdIsNull(employeeId, ChargeStatus.CONFIRMED));
    }

    @Override public List<ServiceCharge> findOpenByBusiness(UUID businessId) {
        return getMapper().toDomainList(jpa.findByBusinessIdAndStatusInAndSettlementIdIsNull(
                businessId, List.of(ChargeStatus.PENDING, ChargeStatus.CONFIRMED)));
    }

    @Override public List<ServiceCharge> findHistory(UUID businessId, Collection<ChargeStatus> statuses,
                                                     LocalDate from, LocalDate to, int page, int size) {
        return getMapper().toDomainList(
                jpa.findByBusinessIdAndStatusInAndServiceDateBetweenOrderByServiceDateDesc(
                        businessId, statuses, from, to, PageRequest.of(page, size)));
    }

    @Override public long countHistory(UUID businessId, Collection<ChargeStatus> statuses,
                                       LocalDate from, LocalDate to) {
        return jpa.countByBusinessIdAndStatusInAndServiceDateBetween(businessId, statuses, from, to);
    }

    @Override public List<ServiceCharge> findBySettlements(UUID employeeId, Collection<UUID> settlementIds) {
        if (employeeId == null || settlementIds == null || settlementIds.isEmpty()) return List.of();
        return getMapper().toDomainList(
                jpa.findByEmployeeIdAndSettlementIdInOrderByServiceDateAscStartTimeAsc(employeeId, settlementIds));
    }
}
