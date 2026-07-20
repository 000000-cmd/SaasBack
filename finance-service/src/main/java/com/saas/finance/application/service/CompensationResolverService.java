package com.saas.finance.application.service;

import com.saas.finance.domain.model.BranchCompensation;
import com.saas.finance.domain.model.BusinessCompensation;
import com.saas.finance.domain.model.CompensationSource;
import com.saas.finance.domain.model.EffectiveCompensation;
import com.saas.finance.domain.model.EmployeeCompensation;
import com.saas.finance.domain.port.in.ICompensationResolverUseCase;
import com.saas.finance.domain.port.out.IBranchCompensationRepositoryPort;
import com.saas.finance.domain.port.out.IBusinessCompensationRepositoryPort;
import com.saas.finance.domain.port.out.IEmployeeCompensationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolucion en cascada de la compensacion efectiva de un empleado.
 *
 * <p>Orden: 1) config vigente del propio empleado; si no tiene, 2) la de su
 * sede; si la sede tampoco, 3) la del negocio. "Vigente" = {@code ValidTo IS
 * NULL}. La empresa/sede fijan una base general y cada nivel inferior la
 * sobreescribe solo como excepcion.</p>
 *
 * <p>Finance resuelve SOLO contra sus propias tablas de compensacion: la
 * cadena empleado->sede->negocio la aporta el caller (los ids llegan en el
 * evento de la cita). No consulta a business, por lo que no hay llamada
 * sincrona ni acoplamiento entre servicios.</p>
 */
@Service
@RequiredArgsConstructor
public class CompensationResolverService implements ICompensationResolverUseCase {

    private final IEmployeeCompensationRepositoryPort employeeCompRepo;
    private final IBranchCompensationRepositoryPort branchCompRepo;
    private final IBusinessCompensationRepositoryPort businessCompRepo;

    @Override
    @Transactional(readOnly = true)
    public Optional<EffectiveCompensation> resolveForEmployee(UUID employeeId, UUID branchId, UUID businessId) {
        // 1) Nivel empleado
        Optional<EmployeeCompensation> emp = employeeCompRepo.findByEmployeeIdAndValidToIsNull(employeeId)
                .stream().findFirst();
        if (emp.isPresent()) {
            EmployeeCompensation c = emp.get();
            return Optional.of(build(CompensationSource.EMPLOYEE, employeeId,
                    c.getCompensationType(), c.getCompensationValue(), c.getSalaryBase(), c.getValidFrom(), c.getValidTo()));
        }

        // 2) Nivel sede (si el caller aporto la sede)
        if (branchId != null) {
            Optional<BranchCompensation> branch = branchCompRepo.findByBranchIdAndValidToIsNull(branchId)
                    .stream().findFirst();
            if (branch.isPresent()) {
                BranchCompensation c = branch.get();
                return Optional.of(build(CompensationSource.BRANCH, branchId,
                        c.getCompensationType(), c.getCompensationValue(), c.getSalaryBase(), c.getValidFrom(), c.getValidTo()));
            }
        }

        // 3) Nivel negocio (si el caller aporto el negocio)
        if (businessId != null) {
            Optional<BusinessCompensation> business = businessCompRepo.findByBusinessIdAndValidToIsNull(businessId)
                    .stream().findFirst();
            if (business.isPresent()) {
                BusinessCompensation c = business.get();
                return Optional.of(build(CompensationSource.BUSINESS, businessId,
                        c.getCompensationType(), c.getCompensationValue(), c.getSalaryBase(), c.getValidFrom(), c.getValidTo()));
            }
        }

        // Ningun nivel tiene configuracion vigente
        return Optional.empty();
    }

    private EffectiveCompensation build(CompensationSource source, UUID sourceId, String type,
                                        BigDecimal value, BigDecimal salaryBase, LocalDateTime validFrom, LocalDateTime validTo) {
        return EffectiveCompensation.builder()
                .source(source).sourceId(sourceId)
                .compensationType(type).compensationValue(value).salaryBase(salaryBase)
                .validFrom(validFrom).validTo(validTo)
                .build();
    }
}
