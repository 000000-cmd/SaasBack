package com.saas.business.application.service;

import com.saas.business.application.dto.request.EmployeeProvisionRequest;
import com.saas.business.application.dto.response.EmployeeProvisionResponse;
import com.saas.business.domain.model.Branch;
import com.saas.business.domain.model.Employee;
import com.saas.business.domain.port.in.IBranchUseCase;
import com.saas.business.domain.port.in.IEmployeeUseCase;
import com.saas.business.infrastructure.client.AuthClient;
import com.saas.business.infrastructure.client.FinanceClient;
import com.saas.business.infrastructure.client.ThirdPartyClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Alta MÍNIMA de un empleado desde el dashboard del dueño:
 * cuenta (auth, rol EMPLOYEE) → tercero shell (solo userId+businessId) →
 * registro laboral shell (solo thirdPartyId+branchId).
 *
 * <p>El empleado completa su información (documento, nombres, cargo…) desde el
 * APK. Orden pensado para minimizar huérfanos: la sede se valida primero y el
 * alta de cuenta falla limpia si username/email ya existen (aún no se creó nada
 * más). Duplicidad 1:1 garantizada por los servicios de dominio: un usuario ↔
 * un tercero (thirdparty) y un tercero ↔ un empleado (aquí).</p>
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
    private final FinanceClient financeClient;

    @Transactional
    public EmployeeProvisionResponse provision(EmployeeProvisionRequest r) {
        // 1) La sede debe existir (404 si no) y aporta el businessId del contexto.
        Branch branch = branchUseCase.getById(r.branchId());

        // 2) Cuenta con rol EMPLOYEE (username/email duplicados fallan aquí,
        //    antes de crear cualquier otra cosa).
        AuthClient.CreatedUser account = authClient.createUser(new AuthClient.CreateUserRequest(
                r.username(), r.email(), null, null,
                r.password(), Set.of(EMPLOYEE_ROLE_CODE)));

        // 3) Tercero SHELL: solo las FKs que reservan el cupo (userId+businessId).
        //    thirdparty valida 1:1 usuario↔tercero.
        ThirdPartyClient.PersonResponse person = thirdPartyClient.createPerson(
                new ThirdPartyClient.CreatePersonRequest(
                        null, null, account.id(), branch.getBusinessId(),
                        null, null, null, null, null, null, null));

        // 4) Registro laboral SHELL: tercero+sede. EmployeeService valida 1:1
        //    tercero↔empleado.
        Employee employee = employeeUseCase.create(Employee.builder()
                .thirdPartyId(person.id())
                .branchId(branch.getId())
                .build());

        // 5) Saldo por cobrar en 0 (materializado + proyectado a ES). Tolerante:
        //    si finance no responde, el saldo se creará en la primera consulta/cálculo.
        try {
            financeClient.ensureBalance(new FinanceClient.EnsureBalanceRequest(
                    employee.getId(), branch.getBusinessId(), branch.getId(), person.id(), account.id()));
        } catch (Exception ex) {
            log.warn("No se pudo inicializar el saldo del empleado {}: {}", employee.getId(), ex.getMessage());
        }

        log.info("Empleado (shell) aprovisionado: employeeId={} thirdPartyId={} userId={}",
                employee.getId(), person.id(), account.id());
        return new EmployeeProvisionResponse(employee.getId(), person.id(), account.id(), account.username());
    }
}
