package com.saas.finance.domain.port.in;

import com.saas.finance.domain.model.EmployeeBalance;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IEmployeeBalanceUseCase {

    /** Crea el saldo en 0 si no existe (o actualiza el contexto), recalcula y proyecta a ES. */
    EmployeeBalance ensure(UUID employeeId, UUID businessId, UUID branchId, UUID thirdPartyId, UUID userId);

    /** Recalcula el saldo del empleado desde sus fuentes y lo proyecta a ES. No-op si no existe la fila. */
    Optional<EmployeeBalance> recalculate(UUID employeeId);

    /** Suma {@code amount} al devengado (sube el por cobrar): el empleado GANA. */
    EmployeeBalance registerCredit(UUID employeeId, java.math.BigDecimal amount);

    /** Suma {@code amount} a lo pagado (baja el por cobrar): el empleado COBRA. */
    EmployeeBalance registerPayment(UUID employeeId, java.math.BigDecimal amount);

    /**
     * Deshace un pago: BAJA lo pagado, asi que el por cobrar vuelve a subir.
     *
     * <p>Metodo propio y no un {@code registerPayment} con importe negativo:
     * "pagar menos veintemil" no es una operacion que exista en un libro, y el
     * dia que alguien lo lea tendria que deducir que quiso decir. Ademas aqui
     * cabe la unica guarda que importa — lo pagado no puede quedar negativo.</p>
     */
    EmployeeBalance registerPaymentReversal(UUID employeeId, java.math.BigDecimal amount);

    Optional<EmployeeBalance> findByEmployee(UUID employeeId);
    Optional<EmployeeBalance> findByUser(UUID userId);

    /** Un saldo por su id. Lo usa el reindex puntual desde la gestión de Elastic. */
    Optional<EmployeeBalance> findById(UUID id);

    /** Página de saldos para el reindex completo de search-service. */
    List<EmployeeBalance> findAllPaged(int page, int size);

    /** Total de saldos (lo usa el reindex para saber cuántos trae). */
    long count();
}
