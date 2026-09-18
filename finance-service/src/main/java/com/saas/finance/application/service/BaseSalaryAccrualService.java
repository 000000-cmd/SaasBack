package com.saas.finance.application.service;

import com.saas.finance.domain.model.*;
import com.saas.finance.domain.port.in.IBusinessCompensationUseCase;
import com.saas.finance.domain.port.in.ICompensationResolverUseCase;
import com.saas.finance.domain.port.in.IEmployeeBalanceUseCase;
import com.saas.finance.domain.port.out.IEmployeeBalanceRepositoryPort;
import com.saas.finance.domain.port.out.IEmployeeSettlementRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Abono programado del sueldo base.
 *
 * <p>Un empleado con sueldo base no gana solo por servicios: al inicio de cada
 * periodo de nomina ya tiene comprometida su parte fija. Si no se le abona, su
 * saldo miente — dice cero cuando en realidad se le debe el sueldo — y el dia de
 * la dispersion aparece un numero que nunca vio venir.</p>
 *
 * <p>Se abona la FRACCION que corresponde al periodo: con nomina quincenal y un
 * sueldo de 1.000.000, al empleado le aparecen 500.000 al inicio de cada
 * quincena. El saldo debe reflejar lo que se le va a consignar, no lo que
 * ganara a fin de mes.</p>
 *
 * <p>La idempotencia NO depende de que la tarea corra una sola vez: la clave
 * unica (EmployeeId, MovementType, PeriodKey) impide el doble abono aunque la
 * tarea se dispare mil veces, se reinicie el servicio o alguien la lance a
 * mano. Correr de mas es inofensivo por diseno.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BaseSalaryAccrualService {

    private final IEmployeeBalanceRepositoryPort balancesRepo;
    private final IEmployeeSettlementRepositoryPort movements;
    private final IEmployeeBalanceUseCase balances;
    private final ICompensationResolverUseCase resolver;
    private final IBusinessCompensationUseCase businessComp;
    /**
     * Transaccion POR EMPLEADO, abierta a mano.
     *
     * <p>Anotar el metodo no serviria: se llama desde el bucle de esta misma
     * clase y Spring no pasa por el proxy en una autoinvocacion, asi que la
     * anotacion se ignoraria EN SILENCIO. Y una unica transaccion para todos
     * seria peor: que a un empleado le falte la compensacion tumbaria el abono
     * de los demas.</p>
     */
    private final TransactionTemplate tx;

    /**
     * Cada dia de madrugada. Diario y no mensual a proposito: si el servicio
     * estuvo caido el dia 1, al dia siguiente se pone al dia solo, y el que ya
     * cobro su periodo no se abona otra vez.
     */
    @Scheduled(cron = "${saas.finance.base-salary-cron:0 10 3 * * *}")
    public void scheduled() {
        int credited = accrue(LocalDate.now());
        if (credited > 0) log.info("Sueldo base abonado a {} colaborador(es)", credited);
    }

    /**
     * Abona el sueldo base del periodo que contiene a {@code today} a todo el que
     * tenga uno configurado. Devuelve cuantos se abonaron.
     *
     * <p>ponytail: recorre todos los saldos de una vez (una fila por empleado).
     * Si esto llegara a cientos de miles, pasarlo a paginas por negocio.</p>
     */
    public int accrue(LocalDate today) {
        Map<UUID, PayrollFrequency> frequencyCache = new HashMap<>();
        int credited = 0;

        for (EmployeeBalance balance : balancesRepo.findAll()) {
            PayrollFrequency frequency = frequencyCache.computeIfAbsent(
                    balance.getBusinessId(), this::frequencyOf);
            try {
                Boolean done = tx.execute(status -> creditOne(balance, frequency, today));
                if (Boolean.TRUE.equals(done)) credited++;
            } catch (DataIntegrityViolationException | UnexpectedRollbackException dup) {
                // Otra instancia se adelanto. La clave unica
                // (EmployeeId, MovementType, PeriodKey) hizo su trabajo: nadie
                // cobro dos veces. NO es un fallo, y decir que lo es en el log
                // manda a alguien a investigar el dia que corran dos replicas.
                log.debug("Sueldo base de {} ya abonado para {}",
                        balance.getEmployeeId(), frequency.periodKey(today));
            } catch (RuntimeException ex) {
                log.warn("No se pudo abonar el sueldo base de {}: {}",
                        balance.getEmployeeId(), ex.getMessage());
            }
        }
        return credited;
    }

    private boolean creditOne(EmployeeBalance balance, PayrollFrequency frequency, LocalDate today) {
        UUID employeeId = balance.getEmployeeId();
        BigDecimal monthlyBase = monthlyBaseOf(balance);
        if (monthlyBase.compareTo(BigDecimal.ZERO) <= 0) return false;

        String periodKey = frequency.periodKey(today);
        if (movements.existsPeriodMovement(employeeId, MovementType.BASE_SALARY, periodKey)) return false;

        BigDecimal amount = monthlyBase.divide(
                BigDecimal.valueOf(frequency.getPeriodsPerMonth()), 2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return false;

        BigDecimal before = balance.getBalance() == null ? BigDecimal.ZERO : balance.getBalance();

        // El choque de clave unica NO se captura aqui: para cuando llegaria, la
        // transaccion ya esta marcada para deshacerse, y ademas Hibernate puede
        // no volcar el INSERT hasta el commit — o sea que muchas veces ni
        // siquiera llegaria. Se maneja FUERA, alrededor del tx.execute, que es
        // donde el choque se ve de verdad.
        movements.save(EmployeeSettlement.builder()
                .businessId(balance.getBusinessId())
                .branchId(balance.getBranchId())
                .employeeId(employeeId)
                .amount(amount)
                .balanceBefore(before)
                .currency(balance.getCurrency())
                .settledAt(LocalDateTime.now())
                .note("Sueldo base · " + frequency.periodLabel(today))
                .movementType(MovementType.BASE_SALARY)
                .periodKey(periodKey)
                .commissionAmount(BigDecimal.ZERO)
                .baseSalaryAmount(amount)
                .build());

        balances.registerCredit(employeeId, amount);
        return true;
    }

    /**
     * Sueldo base MENSUAL del empleado segun su compensacion efectiva.
     *
     * <p>Los tipos hibridos lo guardan en {@code salaryBase} (el porcentaje va en
     * {@code compensationValue}); "Solo salario" no es hibrido y su salario ES
     * {@code compensationValue}. Confundir los dos campos abonaria un porcentaje
     * como si fueran pesos.</p>
     */
    private BigDecimal monthlyBaseOf(EmployeeBalance balance) {
        EffectiveCompensation comp = resolver
                .resolveForEmployee(balance.getEmployeeId(), balance.getBranchId(), balance.getBusinessId())
                .orElse(null);
        if (comp == null) return BigDecimal.ZERO;

        BigDecimal base = "SALARY_ONLY".equals(comp.getCompensationType())
                ? comp.getCompensationValue()
                : comp.getSalaryBase();
        return base == null ? BigDecimal.ZERO : base;
    }

    private PayrollFrequency frequencyOf(UUID businessId) {
        return businessComp.findCurrentByBusiness(businessId)
                .map(c -> PayrollFrequency.from(c.getPayrollFrequency()))
                .orElse(PayrollFrequency.MONTHLY);
    }
}
