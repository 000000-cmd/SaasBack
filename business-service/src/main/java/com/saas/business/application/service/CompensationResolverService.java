package com.saas.business.application.service;

import com.saas.business.domain.model.*;
import com.saas.business.domain.port.in.ICompensationResolverUseCase;
import com.saas.business.domain.port.out.IBranchCompensationRepositoryPort;
import com.saas.business.domain.port.out.IBranchRepositoryPort;
import com.saas.business.domain.port.out.IBusinessCompensationRepositoryPort;
import com.saas.business.domain.port.out.IEmployeeCompensationRepositoryPort;
import com.saas.business.domain.port.out.IEmployeeRepositoryPort;
import com.saas.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolucion en cascada de la compensacion efectiva de un empleado.
 *
 * <p>Orden: 1) config vigente del propio empleado; si no tiene, 2) la de su
 * sede; si la sede tampoco, 3) la del negocio. "Vigente" = {@code ValidTo IS
 * NULL}. La empresa/sede pueden fijar una base general y cada nivel inferior
 * la sobreescribe solo como excepcion.</p>
 */
@Service
@RequiredArgsConstructor
public class CompensationResolverService implements ICompensationResolverUseCase {

    private final IEmployeeCompensationRepositoryPort employeeCompRepo;
    private final IBranchCompensationRepositoryPort branchCompRepo;
    private final IBusinessCompensationRepositoryPort businessCompRepo;
    private final IEmployeeRepositoryPort employeeRepo;
    private final IBranchRepositoryPort branchRepo;

    @Override
    @Transactional(readOnly = true)
    public Optional<EffectiveCompensation> resolveForEmployee(UUID employeeId) {
        // 1) Nivel empleado
        Optional<EmployeeCompensation> emp = employeeCompRepo.findByEmployeeIdAndValidToIsNull(employeeId)
                .stream().findFirst();
        if (emp.isPresent()) {
            EmployeeCompensation c = emp.get();
            return Optional.of(build(CompensationSource.EMPLOYEE, employeeId,
                    c.getCompensationType(), c.getCompensationValue(), c.getValidFrom(), c.getValidTo()));
        }

        // Escala a la sede del empleado
        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", "id", employeeId));
        UUID branchId = employee.getBranchId();

        // 2) Nivel sede
        Optional<BranchCompensation> branch = branchCompRepo.findByBranchIdAndValidToIsNull(branchId)
                .stream().findFirst();
        if (branch.isPresent()) {
            BranchCompensation c = branch.get();
            return Optional.of(build(CompensationSource.BRANCH, branchId,
                    c.getCompensationType(), c.getCompensationValue(), c.getValidFrom(), c.getValidTo()));
        }

        // Escala al negocio de la sede
        Branch branchEntity = branchRepo.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Sede", "id", branchId));
        UUID businessId = branchEntity.getBusinessId();

        // 3) Nivel negocio
        Optional<BusinessCompensation> business = businessCompRepo.findByBusinessIdAndValidToIsNull(businessId)
                .stream().findFirst();
        if (business.isPresent()) {
            BusinessCompensation c = business.get();
            return Optional.of(build(CompensationSource.BUSINESS, businessId,
                    c.getCompensationType(), c.getCompensationValue(), c.getValidFrom(), c.getValidTo()));
        }

        // Ningun nivel tiene configuracion vigente
        return Optional.empty();
    }

    private EffectiveCompensation build(CompensationSource source, UUID sourceId, String type,
                                        java.math.BigDecimal value,
                                        java.time.LocalDateTime validFrom,
                                        java.time.LocalDateTime validTo) {
        return EffectiveCompensation.builder()
                .source(source).sourceId(sourceId)
                .compensationType(type).compensationValue(value)
                .validFrom(validFrom).validTo(validTo)
                .build();
    }
}
