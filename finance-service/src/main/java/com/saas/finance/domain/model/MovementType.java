package com.saas.finance.domain.model;

/**
 * Direccion del dinero en el extracto del empleado.
 *
 * <p>Liquidar y pagar no son lo mismo y hasta ahora estaban mezclados. Liquidar
 * aprueba trabajo y ABONA al saldo del colaborador; dispersar nomina saca ese
 * saldo de la caja de la empresa y se lo CONSIGNA. Este tipo es lo que permite
 * que el empleado distinga "ya me lo reconocieron" de "ya me lo pagaron".</p>
 */
public enum MovementType {
    /** (+) Liquidacion de servicios aprobados. Sube el devengado. */
    COMMISSION,
    /** (+) Abono programado del sueldo base del periodo. Sube el devengado. */
    BASE_SALARY,
    /** (-) Dispersion de nomina: la plata sale de la empresa. Sube lo pagado. */
    PAYROLL,

    /**
     * (+) Deshace una dispersion. BAJA lo pagado, asi que el saldo por cobrar
     * vuelve a subir.
     *
     * <p>Existe porque el libro es de solo anadir: borrar la fila del pago
     * haria desaparecer que ese dia salio plata, y eso paso. El saldo vuelve
     * por la SUMA de los dos movimientos.</p>
     */
    PAYROLL_REVERSAL;

    /** Los abonos suman al devengado; la nomina suma a lo pagado. */
    public boolean isCredit() {
        return this != PAYROLL;
    }

    /** Deshace otro movimiento en vez de registrar un hecho nuevo. */
    public boolean isReversal() {
        return this == PAYROLL_REVERSAL;
    }
}
