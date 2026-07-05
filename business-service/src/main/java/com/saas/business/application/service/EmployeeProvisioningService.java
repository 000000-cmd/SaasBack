package com.saas.business.application.service;

import com.saas.business.application.dto.request.EmployeeProvisionRequest;
import com.saas.business.application.dto.response.EmployeeProvisionResponse;
import com.saas.business.domain.model.Branch;
import com.saas.business.domain.model.Employee;
import com.saas.business.domain.port.in.IBranchUseCase;
import com.saas.business.domain.port.in.IEmployeeUseCase;
import com.saas.business.infrastructure.client.AuthClient;
import com.saas.business.infrastructure.client.ThirdPartyClient;
import com.saas.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Orquesta el alta COMPLETA de un empleado desde el dashboard del dueño:
 * cuenta (auth, rol EMPLOYEE) → persona (thirdparty) → registro laboral (local).
 *
 * <p>Orden pensado para minimizar huérfanos: primero se valida la sede y el
 * duplicado de documento (pre-check S2S); el alta de cuenta falla limpia si el
 * username/email ya existen (aún no se creó nada más). Igual que el provisioning
 * del negocio, no hay saga de compensación todavía: si la persona falla después
 * de crear la cuenta, la cuenta queda huérfana (mejora futura).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeProvisioningService {

    private static final String EMPLOYEE_ROLE_CODE = "EMPLOYEE";

    private final IBranchUseCase branchUseCase;
    private final IEmployeeUseCase employeeUseCase;
    private final AuthClient authClient;
    private final ThirdPartyClient thirdPartyClient;

    @Transactional
    public EmployeeProvisionResponse provision(EmployeeProvisionRequest r) {
        // 1) La sede debe existir (404 si no) y da el businessId del contexto.
        Branch branch = branchUseCase.getById(r.branchId());

        // 2) Pre-check de documento duplicado (evita cuentas huérfanas).
        Boolean exists = thirdPartyClient
                .documentExists(r.documentTypeId(), r.documentNumber())
                .getOrDefault("exists", false);
        if (Boolean.TRUE.equals(exists)) {
            throw new BusinessException("Ya existe una persona con ese documento");
        }

        // 3) Cuenta con rol EMPLOYEE (por código; auth resuelve el id sembrado).
        AuthClient.CreatedUser account = authClient.createUser(new AuthClient.CreateUserRequest(
                r.username(), r.email(), r.firstName(), r.firstLastName(),
                r.password(), Set.of(EMPLOYEE_ROLE_CODE)));

        // 4) Persona vinculada a la cuenta y al negocio de la sede.
        ThirdPartyClient.PersonResponse person = thirdPartyClient.createPerson(
                new ThirdPartyClient.CreatePersonRequest(
                        r.documentTypeId(), r.documentNumber(), account.id(), branch.getBusinessId(),
                        r.firstName(), r.secondName(), r.firstLastName(), r.secondLastName(),
                        r.genderId(), r.birthDate(), null));

        // 5) Registro laboral en la sede.
        Employee employee = employeeUseCase.create(Employee.builder()
                .thirdPartyId(person.id())
                .branchId(branch.getId())
                .positionId(r.positionId())
                .employeeCode(r.employeeCode())
                .hireDate(r.hireDate())
                .build());

        log.info("Empleado aprovisionado: employeeId={} thirdPartyId={} userId={}",
                employee.getId(), person.id(), account.id());
        return new EmployeeProvisionResponse(employee.getId(), person.id(), account.id(), account.username());
    }
}
